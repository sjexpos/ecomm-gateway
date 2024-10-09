package io.oigres.ecomm.gateway.filter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.oigres.ecomm.gateway.util.SignInResponse;
import io.oigres.ecomm.gateway.validator.RouteValidator;
import io.oigres.ecomm.service.users.api.model.ValidateUserRequest;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityScheme.In;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * This filter intercepts the request from Open Api 3.0 specification for the
 * protected API,
 * and enhances it with security schema, authorization error response and adds
 * the sign-in endpoint.
 * 
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Component
@Slf4j
public class OpenApiEnhacementRewriteFunction implements RewriteFunction<String, String> {

    public static class SecuritySchemeTypeSerializer extends StdSerializer<SecurityScheme.Type> {
        public SecuritySchemeTypeSerializer() {
            super(SecurityScheme.Type.class);
        }

        @Override
        public void serialize(Type value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            if (value != null) {
                generator.writeString(value.toString());
            }
        }
    }

    public static class SecuritySchemeInSerializer extends StdSerializer<SecurityScheme.In> {
        public SecuritySchemeInSerializer() {
            super(SecurityScheme.In.class);
        }

        @Override
        public void serialize(In value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            if (value != null) {
                generator.writeString(value.toString());
            }
        }
    }

    private final ObjectMapper mapper;
    private final Environment environment;
    private final int gatewayPort;
    private final String gatewayName;

    public OpenApiEnhacementRewriteFunction(Environment environment) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(new SecuritySchemeTypeSerializer());
        module.addSerializer(new SecuritySchemeInSerializer());
        ObjectMapper localMapper = new ObjectMapper();
        localMapper.setSerializationInclusion(Include.NON_NULL);
        localMapper.registerModule(module);
        this.mapper = localMapper;
        this.environment = environment;
        this.gatewayPort = Integer.parseInt(environment.getProperty("server.port", "8080"));
        this.gatewayName = InetAddress.getLoopbackAddress().getHostName();
    }

    @Override
    public Publisher<String> apply(ServerWebExchange t, String responseBody) {
        try {
            OpenAPI api = mapper.readValue(responseBody, OpenAPI.class);
            enhaceOpenApiDescription(api);
            responseBody = mapper.writeValueAsString(api);
        } catch (JsonMappingException e) {
            log.error("OpenApi Enhacement failed: ", e);
        } catch (JsonProcessingException e) {
            log.error("OpenApi Enhacement failed: ", e);
        }
        return Mono.just(responseBody);
    }

    private void enhaceOpenApiDescription(OpenAPI api) {
        addSecurityScheme(api);
        addIfNotPresent401Response(api.getPaths());
        updateServersByGateway(api.getServers());
        addSignInOperation(api.getPaths(), api.getComponents());
    }

    private void addSecurityScheme(OpenAPI api) {
        SecurityScheme securitySchemeItem = createBearerAuthSecurityScheme();
        api.schemaRequirement("globalAuth", securitySchemeItem);
        SecurityRequirement securityItem = new SecurityRequirement();
        securityItem.addList("globalAuth", "global");
        api.addSecurityItem(securityItem);
    }

    private SecurityScheme createBearerAuthSecurityScheme() {
        SecurityScheme securityScheme = new SecurityScheme();
        securityScheme.setType(SecurityScheme.Type.HTTP);
        securityScheme.setScheme("bearer");
        securityScheme.setBearerFormat("JWT");
        securityScheme.setIn(SecurityScheme.In.HEADER);
        securityScheme.setName(HttpHeaders.AUTHORIZATION);
        return securityScheme;
    }

    private void addIfNotPresent401Response(Paths paths) {
        paths.forEach((key, item) -> {
            addIfNotPresent401Response(item.getGet());
            addIfNotPresent401Response(item.getPut());
            addIfNotPresent401Response(item.getPost());
            addIfNotPresent401Response(item.getDelete());
            addIfNotPresent401Response(item.getOptions());
            addIfNotPresent401Response(item.getHead());
            addIfNotPresent401Response(item.getPatch());
            addIfNotPresent401Response(item.getTrace());
        });
    }

    private void addIfNotPresent401Response(Operation op) {
        if (op != null) {
            if (op.getResponses() == null) {
                op.setResponses(new ApiResponses());
            }
            if (!op.getResponses().containsKey(Integer.toString(HttpStatus.UNAUTHORIZED.value()))) {
                MediaType mediaType = new MediaType();
                Content content = new Content();
                content.addMediaType(MimeTypeUtils.APPLICATION_JSON_VALUE, mediaType);
                ApiResponse apiResponse = new ApiResponse();
                apiResponse.setDescription("Unauthorized: JWT is needed");
                apiResponse.setContent(content);
                op.getResponses().addApiResponse(Integer.toString(HttpStatus.UNAUTHORIZED.value()), apiResponse);
            }
        }
    }

    private void updateServersByGateway(List<Server> servers) {
        if (servers != null) {
            servers.forEach(this::updateServerByGateway);
        }
    }

    private void updateServerByGateway(Server server) {
        if (server.getUrl() != null) {
            server.setDescription(null);
            String scheme = this.environment.containsProperty("server.ssl") ? "https" : "http";
            String newUrl = String.format("%s://%s:%d", scheme, this.gatewayName, this.gatewayPort);
            server.setUrl(newUrl);
        }
    }

    private Schema<Object> buildSchemaFor(Object objectComponent) {
        Schema<Object> objectSchema = new Schema<>();
        ObjectNode objectJson = this.mapper.convertValue(objectComponent, ObjectNode.class);
        List<String> requiredFields = new LinkedList<>();
        Function<JsonNode, String> typeTransform = node -> node != null ? node.getNodeType().name().toLowerCase() : "";
        objectJson.fields().forEachRemaining(property -> {
            objectSchema.addProperty(property.getKey(),
                    new Schema<>().format(typeTransform.apply(property.getValue())));
            requiredFields.add(property.getKey());
        });
        return objectSchema;
    }

    private static final String EMPTY_VALUE = "empty";

    private void addSignInOperation(Paths paths, Components components) {
        if (paths != null) {
            ValidateUserRequest validateUserRequest = ValidateUserRequest.builder()
                    .email(EMPTY_VALUE)
                    .password(EMPTY_VALUE)
                    .build();
            Schema<Object> signinRequestSchema = buildSchemaFor(validateUserRequest);
            SignInResponse signInResponse = SignInResponse.builder()
                    .userid(EMPTY_VALUE)
                    .name(EMPTY_VALUE)
                    .token(EMPTY_VALUE)
                    .build();
            Schema<Object> signinResponseSchema = buildSchemaFor(signInResponse);
            components.addSchemas("SignInRequest", signinRequestSchema);
            components.addSchemas("SignInResponse", signinResponseSchema);
            PathItem pathItem = new PathItem();
            Operation post = new Operation()
                    .operationId("signinop")
                    .addTagsItem("Auth")
                    .summary("Sign-In")
                    .requestBody(
                            new RequestBody()
                                    .content(
                                            new Content()
                                                    .addMediaType(
                                                            MimeTypeUtils.APPLICATION_JSON_VALUE,
                                                            new MediaType()
                                                                    .schema(
                                                                            new Schema<>().$ref(
                                                                                    "#/components/schemas/SignInRequest")))))
                    .responses(
                            new ApiResponses()
                                    .addApiResponse(
                                            Integer.toString(HttpStatus.OK.value()),
                                            new ApiResponse()
                                                    .content(
                                                            new Content()
                                                                    .addMediaType(
                                                                            MimeTypeUtils.APPLICATION_JSON_VALUE,
                                                                            new MediaType()
                                                                                    .schema(
                                                                                            new Schema<>().$ref(
                                                                                                    "#/components/schemas/SignInResponse")))))
                                    .addApiResponse(
                                            Integer.toString(HttpStatus.BAD_REQUEST.value()),
                                            new ApiResponse()
                                                    .content(
                                                            new Content()
                                                                    .addMediaType(
                                                                            MimeTypeUtils.APPLICATION_JSON_VALUE,
                                                                            new MediaType()))));
            pathItem.post(post);

            LinkedHashMap<String, PathItem> oldItems = new LinkedHashMap<>();
            oldItems.putAll(paths);
            paths.clear();
            paths.addPathItem(RouteValidator.SIGNIN_PATH, pathItem);
            paths.putAll(oldItems);
        }
    }

}
