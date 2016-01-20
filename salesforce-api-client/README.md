Salesforce API Client
=====================================
This module contains functionality for integrating with Salesforce's API.

## Integrating into a Scala application
1.  Add library to managed dependencies in build.sbt.

    ```scala
    libraryDependencies += "kipsigman.ws" %% "salesforce-api-client" % "0.1.0"
    ```

2.  Create a SalesforceService in your app.

    ```scala
    import kipsigman.ws.salesforce.SalesforceService
    import kipsigman.ws.salesforce.actor.SalesforceActorService
    
    // Guice
    bind(classOf[SalesforceService]).to(classOf[SalesforceActorService])
    ```

3.  Configuration
    You will need to define the following environment variables.

    ```sh
    export SFDC_APP_ID="<appId>"
    export SFDC_APP_SECRET="<appSecret>"
    export SFDC_OAUTH_HOST="https://login.salesforce.com"
    export SFDC_PASSTOKEN="<passtoken>"
    export SFDC_USER="<user>"
    ```