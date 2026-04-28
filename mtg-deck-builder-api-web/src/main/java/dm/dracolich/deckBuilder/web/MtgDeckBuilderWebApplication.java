package dm.dracolich.deckBuilder.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;


@SpringBootApplication
@ComponentScan(basePackages = {"dm.dracolich.deckBuilder.web", "dm.dracolich.deckBuilder.core",
        "dm.dracolich.deckBuilder.integration", "dm.dracolich.forge"})
@EnableReactiveMongoRepositories(basePackages = {"dm.dracolich.deckBuilder.data.repository"})
@OpenAPIDefinition(
        info = @Info(title = "Dracolich MTG Deck Builder API", version = "v0",
                description = "Deck CRUD, favorites, format rules engine, AI bridge"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class MtgDeckBuilderWebApplication {
    public static void main(String[] args) { SpringApplication.run(MtgDeckBuilderWebApplication.class, args); }
}
