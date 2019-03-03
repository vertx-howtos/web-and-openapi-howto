package io.vertx.howtos.openapi;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.ext.web.api.contract.RouterFactoryOptions;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.Router;
import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class APIVerticle extends AbstractVerticle {

  HttpServer server;

  final List<JsonObject> pets = new ArrayList<>(Arrays.asList(
    new JsonObject().put("id", 1).put("name", "Fufi").put("tag", "ABC"),
    new JsonObject().put("id", 2).put("name", "Garfield").put("tag", "XYZ"),
    new JsonObject().put("id", 3).put("name", "Puffa")
  ));

  @Override
  public void start(Future<Void> future) {
    OpenAPI3RouterFactory.create(this.vertx, "petstore.yaml", ar -> {
      if (ar.succeeded()) {
        OpenAPI3RouterFactory routerFactory = ar.result();

        // Add routes handlers
        // tag::listPetsHandler[]
        routerFactory.addHandlerByOperationId("listPets", routingContext ->
          routingContext
            .response() // <1>
            .setStatusCode(200)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json") // <2>
            .end(new JsonArray(getAllPets()).encode()) // <3>
        );
        // end::listPetsHandler[]
        // tag::createPetsHandler[]
        routerFactory.addHandlerByOperationId("createPets", routingContext -> {
          RequestParameters params = routingContext.get("parsedParameters"); // <1>
          JsonObject pet = params.body().getJsonObject(); // <2>
          addPet(pet);
          routingContext
            .response()
            .setStatusCode(200)
            .end(); // <3>
        });
        // end::createPetsHandler[]
        // tag::showPetByIdHandler[]
        routerFactory.addHandlerByOperationId("showPetById", routingContext -> {
          RequestParameters params = routingContext.get("parsedParameters"); // <1>
          Integer id = params.pathParameter("petId").getInteger(); // <2>
          Optional<JsonObject> pet = getAllPets()
            .stream()
            .filter(p -> p.getInteger("id").equals(id))
            .findFirst(); // <3>
          if (pet.isPresent())
            routingContext
              .response()
              .setStatusCode(200)
              .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
              .end(pet.get().encode()); // <4>
          else
            routingContext.fail(404, new Exception("Pet not found")); // <5>
        });
        // end::showPetByIdHandler[]

        // Generate the router
        // tag::routerGen[]
        Router router = routerFactory.getRouter(); // <1>
        router.errorHandler(404, routingContext -> { // <2>
          JsonObject errorObject = new JsonObject() // <3>
            .put("code", 404)
            .put("message",
              (routingContext.failure() != null) ?
                routingContext.failure().getMessage() :
                "Not Found"
            );
          routingContext
            .response()
            .setStatusCode(404)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(errorObject.encode()); // <4>
        });
        router.errorHandler(400, routingContext -> {
          JsonObject errorObject = new JsonObject()
            .put("code", 400)
            .put("message",
              (routingContext.failure() != null) ?
                routingContext.failure().getMessage() :
                "Validation Exception"
            );
          routingContext
            .response()
            .setStatusCode(400)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(errorObject.encode());
        });

        server = vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost")); // <5>
        server.requestHandler(router).listen(); // <6>
        // end::routerGen[]
        future.complete(); // Complete the verticle start
      } else {
        future.fail(ar.cause()); // Fail the verticle start
      }
    });
  }

  @Override
  public void stop(){
    this.server.close();
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new APIVerticle());
  }

  private List<JsonObject> getAllPets() {
    return this.pets;
  }

  private void addPet(JsonObject pet) {
    this.pets.add(pet);
  }

  // tag::loadSpecSampleMethod[]
  // For documentation purpose
  private void loadSpecSample(Future<Void> future) {
    // tag::loadSpec[]
    OpenAPI3RouterFactory.create(this.vertx, "petstore.yaml", ar -> {
      if (ar.succeeded()) {
        OpenAPI3RouterFactory routerFactory = ar.result(); // <1>
      } else {
        // Something went wrong during router factory initialization
        future.fail(ar.cause()); // <2>
      }
    });
    // end::loadSpec[]
  }
  // end::loadSpecSampleMethod[]
}
