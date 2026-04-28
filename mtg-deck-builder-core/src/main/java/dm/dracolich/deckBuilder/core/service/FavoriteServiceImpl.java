package dm.dracolich.deckBuilder.core.service;

import dm.dracolich.deckBuilder.data.entity.DeckEntity;
import dm.dracolich.deckBuilder.data.entity.FavoriteEntity;
import dm.dracolich.deckBuilder.data.repository.DeckRepository;
import dm.dracolich.deckBuilder.data.repository.FavoriteRepository;
import dm.dracolich.forge.security.Principal;
import dm.dracolich.forge.security.ReactiveSecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

import static dm.dracolich.deckBuilder.core.helpers.ErrorUtil.notFound;
import static dm.dracolich.deckBuilder.core.helpers.ErrorUtil.unauthorized;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository repo;
    private final DeckRepository deckRepo;

    @Override
    public Mono<Boolean> favoriteDeck(String deckId) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .switchIfEmpty(Mono.error(unauthorized()))
                .flatMap(principal -> buildDeckFavorite(principal, deckId, true));
    }

    @Override
    public Mono<Boolean> unfavoriteDeck(String deckId) {
        return ReactiveSecurityContextUtil.getPrincipal()
                .switchIfEmpty(Mono.error(unauthorized()))
                .flatMap(principal -> buildDeckFavorite(principal, deckId, false));
    }

    private Mono<Boolean> buildDeckFavorite(Principal principal, String deckId, boolean fave) {
        if (principal == null || principal.isAnon()) {
            return Mono.error(unauthorized());
        }

        if (fave) {
            return buildFavorite(principal, deckId);
        } else {
            return buildUnfavorite(principal, deckId);
        }
    }

    private Mono<Boolean> buildFavorite(Principal principal, String deckId) {
        Mono<FavoriteEntity> saveFavorite = repo.findByUserId(principal.id())
                .flatMap(favorite -> {
                    if (favorite.getDeckIds().contains(deckId)) {
                        return Mono.just(favorite);
                    }
                    favorite.getDeckIds().add(deckId);
                    return repo.save(favorite);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    var entity = FavoriteEntity.builder()
                            .userId(principal.id())
                            .deckIds(new HashSet<>(Set.of(deckId)))
                            .build();
                    return repo.save(entity);
                }));

        return saveFavorite.flatMap(favorite ->
                deckRepo.findById(deckId)
                        .switchIfEmpty(Mono.error(notFound("favorite_deck", "deck", deckId)))
                        .flatMap(deck -> {
                            deck.setFavoritesCount(deck.getFavoritesCount() + 1);
                            return deckRepo.save(deck);
                        }))
                .thenReturn(true);
    }

    private Mono<Boolean> buildUnfavorite(Principal principal, String deckId) {
        Mono<FavoriteEntity> removeFavorite = repo.findByUserId(principal.id())
                .filter(favorite -> favorite.getDeckIds().contains(deckId))
                .flatMap(favorite -> {
                    favorite.getDeckIds().remove(deckId);
                    return repo.save(favorite);
                });

        return removeFavorite.flatMap(__ ->
                deckRepo.findById(deckId)
                        .switchIfEmpty(Mono.error(notFound("unfavorite_deck", "deck", deckId)))
                        .flatMap(deck -> {
                            deck.setFavoritesCount(Math.max(0, deck.getFavoritesCount() - 1));
                            return deckRepo.save(deck);
                        }))
                .thenReturn(true);
    }
}
