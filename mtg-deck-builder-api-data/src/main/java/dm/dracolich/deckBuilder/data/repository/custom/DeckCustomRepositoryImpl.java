package dm.dracolich.deckBuilder.data.repository.custom;

import dm.dracolich.deckBuilder.data.entity.DeckEntity;
import dm.dracolich.deckBuilder.dto.enums.DeckStatus;
import dm.dracolich.deckBuilder.dto.enums.Visibility;
import dm.dracolich.mtgLibrary.dto.enums.Format;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class DeckCustomRepositoryImpl implements DeckCustomRepository {
    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public Mono<Page<DeckEntity>> findUserDecksByFilters(String userId,
                                                         Format format,
                                                         DeckStatus status,
                                                         Visibility visibility,
                                                         int page,
                                                         int size) {
        Criteria criteria = Criteria.where("user_id").is(userId);
        if (format != null) criteria.and("format").is(format);
        if (status != null) criteria.and("status").is(status);
        if (visibility != null) criteria.and("visibility").is(visibility);

        return paginated(criteria, page, size,
                Sort.by(Sort.Direction.DESC, "audit.last_modified"));
    }

    @Override
    public Mono<Page<DeckEntity>> findPopularPublicDecks(Format format, int page, int size) {
        Criteria criteria = Criteria.where("visibility").is(Visibility.PUBLIC);
        if (format != null) criteria.and("format").is(format);

        return paginated(criteria, page, size,
                Sort.by(Sort.Direction.DESC, "favorites_count")
                        .and(Sort.by(Sort.Direction.DESC, "audit.created_at")));
    }

    @Override
    public Mono<Page<DeckEntity>> findLatestPublicDecks(Format format, int page, int size) {
        Criteria criteria = Criteria.where("visibility").is(Visibility.PUBLIC);
        if (format != null) criteria.and("format").is(format);

        return paginated(criteria, page, size,
                Sort.by(Sort.Direction.DESC, "audit.created_at"));
    }

    @Override
    public Mono<Page<DeckEntity>> findPublicDecksByUserId(String userId, Format format, int page, int size) {
        Criteria criteria = Criteria.where("user_id").is(userId)
                .and("visibility").is(Visibility.PUBLIC);
        if (format != null) criteria.and("format").is(format);

        return paginated(criteria, page, size,
                Sort.by(Sort.Direction.DESC, "audit.last_modified"));
    }

    @Override
    public Mono<Page<DeckEntity>> findPublicDecksByIds(Set<String> deckIds, int page, int size) {
        if (deckIds == null || deckIds.isEmpty()) {
            return Mono.just(new PageImpl<>(List.of(), PageRequest.of(page, size), 0));
        }
        Criteria criteria = Criteria.where("_id").in(deckIds)
                .and("visibility").is(Visibility.PUBLIC);

        return paginated(criteria, page, size,
                Sort.by(Sort.Direction.DESC, "audit.last_modified"));
    }

    private Mono<Page<DeckEntity>> paginated(Criteria criteria, int page, int size, Sort sort) {
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Query query = Query.query(criteria);

        return mongoTemplate.count(query, DeckEntity.class)
                .flatMap(total -> mongoTemplate.find(query.with(pageRequest), DeckEntity.class)
                        .collectList()
                        .map(list -> new PageImpl<>(list, pageRequest, total)));
    }
}
