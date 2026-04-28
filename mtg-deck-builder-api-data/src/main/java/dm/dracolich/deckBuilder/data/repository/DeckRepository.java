package dm.dracolich.deckBuilder.data.repository;

import dm.dracolich.deckBuilder.data.entity.DeckEntity;
import dm.dracolich.deckBuilder.data.repository.custom.DeckCustomRepository;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import reactor.core.publisher.Mono;

public interface DeckRepository extends ReactiveMongoRepository<DeckEntity, String>,
        ReactiveQueryByExampleExecutor<DeckEntity>,
        DeckCustomRepository {
    Mono<DeckEntity> findByIdAndUserId(String id, String userId);
    Mono<DeckEntity> findByIdAndAnonId(String id, String anonId);

}
