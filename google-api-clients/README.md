Google API Clients
=====================================
This module contains functionality for integrating with Google APIs.

## Integrating into a Scala application
1.  Add library to managed dependencies in build.sbt.

    ```scala
    libraryDependencies += "kipsigman.ws" %% "google-api-clients" % "0.1.0"
    ```

2.  Create a client in your app.

    ```scala
    import kipsigman.ws.google.youtube.YouTubeApiClient
    
    // Guice
    bind(classOf[YouTubeApiClient])
    ```

3.  Configuration
    You will need to define the following environment variables.

    ```sh
    export GOOGLE_API_KEY=
    export GOOGLE_APPLICATION_NAME=
    export GOOGLE_SERVICE_ACCOUNT_EMAIL=
    export GOOGLE_YOUTUBE_DEFAULT_CHANNEL_ID=
    ```

4.  For AnalyticsApiClient, you'll need a .p12 file and a Google Service Account Email. You learn more about that here - https://console.developers.google.com/project.
