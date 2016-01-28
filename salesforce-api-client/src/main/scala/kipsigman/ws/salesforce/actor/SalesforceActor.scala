package kipsigman.ws.salesforce.actor

import scala.concurrent._
import scala.concurrent.duration._
import scala.language.existentials

import akka.pattern.{ ask, pipe }
import akka.actor._
import com.typesafe.config.Config
import kipsigman.ws.salesforce._

private[actor] object SalesforceActor {

  case class ApiRequest[A](f: (SalesforceApiClient, AccessInfo) => Future[A])

  /**
   * Represents a failure in a request actor due to invalid AccessInfo. Includes the request/sender
   * that was being processed during the failure, as it needs to be re-processed.
   */
  case class AccessInfoInvalid(failedReq: ApiRequest[_], failedAskSender: ActorRef)

  case object RetryRequestAccessInfo

  def props(repository: SalesforceApiClient): Props =
    Props(classOf[SalesforceActor], repository)

  def apply(actorSystem: ActorSystem, config: Config, name: String = "salesforce"): ActorRef = {
    val repository = new SalesforceApiClient(config)(actorSystem.dispatcher)
    actorSystem.actorOf(props(repository), name = name)
  }
}

/**
 * Supervisor Actor to handle requests to the SalesforceRepository. Delegates each request
 * to a new a SalesforceRequestActor.
 */
private[actor] class SalesforceActor(repository: SalesforceApiClient) extends Actor with Stash with ActorLogging {
  import SalesforceActor._
  import context.dispatcher

  var accessInfo: AccessInfo = _

  override def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.stoppingStrategy

  override def preStart(): Unit = {
    log.debug("preStart")
    requestAccessInfo
  }

  override def receive = initializing

  def initializing: Receive = {
    case AccessInfoInvalid(failedReq, failedAskSender) => {
      // Already waiting for new AccessInfo, this must be from an additional child with invalid info
      // Put request that failed back in mailbox to be re-processed
      self.tell(failedReq, failedAskSender)
    }
    case newAccessInfo: AccessInfo => {
      accessInfo = newAccessInfo
      log.debug(s"New accessInfo=$accessInfo")

      log.debug("become processing")
      context.become(processing)
      unstashAll()
    }
    case failure: Status.Failure => {
      // Assume this was an error from requesting AccessInfo
      log.warning(s"receive Status.Failure, retrying requestAccessInfo in 1 minute", failure.cause)
      context.system.scheduler.scheduleOnce(60 seconds, self, RetryRequestAccessInfo)
    }
    case RetryRequestAccessInfo => {
      requestAccessInfo
    }
    case req: ApiRequest[_] => stash()
    case msg: Any => log.warning(s"Unexpected message: $msg")
  }

  def processing: Receive = {
    case AccessInfoInvalid(failedReq, failedAskSender) => {
      log.debug("AccessInfoInvalid, become initializing")
      context.become(initializing)
      // Request new AccessInfo
      requestAccessInfo
      // Put request that failed back in mailbox to be re-processed
      self.tell(failedReq, failedAskSender)
    }
    case req: ApiRequest[_] => {
      log.debug("processing ApiRequest")
      val askSender = context.sender

      // To test access info expiration
      // val flakyInfo =
      // if (scala.util.Random.nextInt(2) == 0) AccessInfo("bogus", accessInfo.instanceUrl)
      // else accessInfo
      // context.actorOf(Props(new SalesforceRequestActor(repository, flakyInfo, req, askSender)))

      // Delegate to child.
      context.actorOf(Props(new SalesforceRequestActor(repository, accessInfo, req, askSender)))
    }
    case msg: Any => log.warning(s"Unexpected message: $msg")
  }

  /**
   * Request accessInfo from Salesforce.
   */
  private def requestAccessInfo: Unit = {
    val accessInfoFuture = repository.refreshAccessInfo
    // piperesult of request to self, will be handled in "initialization"
    pipe(accessInfoFuture)(context.dispatcher) to self
  }
}

/**
 * Actor to handle a single request. Actor is stopped after processing its first message.
 */
private[actor] class SalesforceRequestActor(repository: SalesforceApiClient,
    accessInfo: AccessInfo,
    req: SalesforceActor.ApiRequest[_],
    askSender: ActorRef) extends Actor with ActorLogging {
  import context.dispatcher
  import SalesforceActor._

  override def preStart(): Unit = {
    log.debug("preStart")
    // make request to repository, receive will handle piped result
    val resultFuture = req.f(repository, accessInfo)
    pipe(resultFuture) to context.self
  }

  override def receive = {
    case failure: Status.Failure => {
      log.debug(s"receive Status.Failure cause=${failure.cause}")
      if (failure.cause.getMessage().contains("401") || failure.cause.getMessage().contains("Session expired")) {
        // Assume accessInfo has expired
        log.debug(s"Expired/Invalid accessInfo=$accessInfo")
        context.parent ! AccessInfoInvalid(req, askSender)
      } else {
        askSender ! failure
      }
      context.stop(self)
    }
    case msg: Any => {
      // Assume this is the result piped back to self
      log.debug(s"receive msg:$msg")
      askSender ! msg
      context.stop(self)
    }
  }

  override def postStop(): Unit = {
    log.debug("postStop")
  }
}
