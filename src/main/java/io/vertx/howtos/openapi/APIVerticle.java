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
        routerFactory.addHandlerByOperationId("listPets", routingContext ->
          routingContext
            .response() // <1>
            .setStatusCode(200)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(new JsonArray(getAllPets()).encode())
        );
        routerFactory.addHandlerByOperationId("createPets", routingContext -> {
          RequestParameters params = routingContext.get("parsedParameters");
          JsonObject pet = params.body().getJsonObject();
          addPet(pet);
          routingContext
            .response()
            .setStatusCode(200)
            .end();
        });
        routerFactory.addHandlerByOperationId("showPetById", routingContext -> {
          RequestParameters params = routingContext.get("parsedParameters");
          Integer id = params.pathParameter("petId").getInteger();
          Optional<JsonObject> pet = getAllPets().stream().filter(p -> p.getInteger("id").equals(id)).findFirst();
          if (pet.isPresent())
            routingContext
              .response()
              .setStatusCode(200)
              .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
              .end(pet.get().encode());
          else
            routingContext.fail(404, new Exception("Pet not found"));
        });

        // Generate the router
        Router router = routerFactory.getRouter();
        router.errorHandler(404, routingContext -> {
          JsonObject errorObject = new JsonObject()
            .put("code", 404)
            .put("message", (routingContext.failure() != null) ? routingContext.failure().getMessage() : "Not Found");
          routingContext
            .response()
            .setStatusCode(404)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(errorObject.encode());
        });
        router.errorHandler(400, routingContext -> {
          JsonObject errorObject = new JsonObject()
            .put("code", 400)
            .put("message", (routingContext.failure() != null) ? routingContext.failure().getMessage() : "Validation Exception");
          routingContext
            .response()
            .setStatusCode(400)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(errorObject.encode());
        });

        server = vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost"));
        server.requestHandler(router).listen();
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

}
