# migration-integrity-service
## prerequisites
* OpenJDK 11
* Gradle 6 or higher
* May need lombok plugin which can be found on the vscode or intellij plugin store or downloaded manualy from [here](https://projectlombok.org/download)
## local set up
* to clean and build the project run the command ```gradle clean build```
* to start the service run the command ```gradle bootRun```

Once started by default the local service can be accessed at ```localhost:8080/swagger-ui.html```

Currently there is one controller with one get endpoint that generates an integrity report. The endpoint requires 2 query parameters for the old and new database ports.

#
Provide the required ports for your local old and new databases and execute the rest call. This will query and compare both databases and return a simple csv file that can be download with the results. The given data size during development was too large to display on a swagger page as json so csv was genrated for ease of viewing and speed.

#
The datasource connections are default configured to localhost and PostgreSQL dialect. This would normally be configured inside the application.yml along with CrudRepositories and entities but given the current use case it was quicker and easier to configure both connections and dialect programmatically.