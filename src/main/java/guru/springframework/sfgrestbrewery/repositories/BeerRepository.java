package guru.springframework.sfgrestbrewery.repositories;


import guru.springframework.sfgrestbrewery.domain.Beer;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;


public interface BeerRepository extends ReactiveCrudRepository<Beer, Integer> {
    //THESE DO NOT WORK. MAY IN THE FUTURE
//    Flux<Page<Beer>> findAllByBeerName(String beerName, Pageable pageable);
//
//    Flux<Page<Beer>> findAllByBeerStyle(BeerStyleEnum beerStyle, Pageable pageable);
//
//    Flux<Page<Beer>> findAllByBeerNameAndBeerStyle(String beerName, BeerStyleEnum beerStyle, Pageable pageable);
//
//    Flux<Page<Beer>> findPageBy(Pageable pageable);

    Mono<Beer> findByUpc(String upc);
}
