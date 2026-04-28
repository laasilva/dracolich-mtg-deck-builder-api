package dm.dracolich.deckBuilder.data.repository;

import dm.dracolich.deckBuilder.data.entity.FavoriteEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface FavoriteRepository extends ReactiveMongoRepository<FavoriteEntity, String> {
    Mono<FavoriteEntity> findByUserId(String userId);
}
