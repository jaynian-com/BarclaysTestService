### BARCLAYS 'TAKE HOME CODING TEST' API SERVICE APPLICATION 

#### NOTES:

* To build and run tests run `mvn package`
* To run the service `mvn spring-boot:run`
* REST API stubs have been generated using **org.openapitools openapi-generator** https://github.com/OpenAPITools/openapi-generator (see `pom.xml` )
* Swagger UI is enabled (i.e. http://localhost:8080/swagger-ui/index.html)
* H2 Console is enabled (i.e. http://localhost:8080/h2-console/login.jsp Login: sa/password)
* All scenarios detailed in 'Take home coding test' document should be covered via Mock MVC controller tests (see `src/test/java/com/barclays/testservice/controller/*` )

#### TODO:
* Transaction Reference field is not implemented and hardcoded to 'N/A'
* Have relied on Open API schema generated code to validate invalid input, 
and this works ok in the test, but perhaps some further controller or service layer
validation might be a good idea.

