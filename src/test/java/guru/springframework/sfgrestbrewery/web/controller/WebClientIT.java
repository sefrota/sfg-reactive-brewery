package guru.springframework.sfgrestbrewery.web.controller;

import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
import guru.springframework.sfgrestbrewery.web.model.BeerPagedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by jt on 3/7/21.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class WebClientIT {

    public static final String BASE_URL = "http://localhost:8080";

    WebClient webClient;

    @BeforeEach
    void setUp() {
        webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().wiretap(true)))
                .build();
    }

    @Test
    void getBeerById() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get().uri("/api/v1/beer/1")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beerDto -> {
            assertThat(beerDto).isNotNull();
            assertThat(beerDto.getBeerName()).isNotNull();

            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void getBeerByUpc() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get().uri("/api/v1/beerUpc/" + BeerLoader.BEER_2_UPC)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beerDto -> {
            assertThat(beerDto).isNotNull();
            assertThat(beerDto.getBeerName()).isNotNull();

            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testListBeers() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerPagedList> beerPagedListMono = webClient.get()
                .uri("/api/v1/beer")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerPagedList.class);

        beerPagedListMono.publishOn(Schedulers.parallel()).subscribe(beerPagedList -> {

            beerPagedList.getContent().forEach(beerDto -> System.out.println(beerDto.toString()));

            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testListBeersPageSize5() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerPagedList> beerPagedListMono = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/beer").queryParam("pageSize", 5).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerPagedList.class);

        beerPagedListMono.publishOn(Schedulers.parallel()).subscribe(beerPagedList -> {

            beerPagedList.getContent().forEach(beerDto -> System.out.println(beerDto.toString()));

            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testListBeersByName() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerPagedList> beerPagedListMono = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/beer").queryParam("beerName", "Mango Bobs").build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(BeerPagedList.class);

        beerPagedListMono.publishOn(Schedulers.parallel()).subscribe(beerPagedList -> {

            beerPagedList.getContent().forEach(beerDto -> System.out.println(beerDto.toString()));

            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testSaveBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        BeerDto beerDto = BeerDto.builder()
                .beerName("SLs Beer")
                .beerStyle("PALE_ALE")
                .upc("1233455")
                .price(new BigDecimal("8.99"))
                .build();

        Mono<ResponseEntity<Void>> beerPagedListMono = webClient.post()
                .uri("/api/v1/beer")
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(beerDto))
                .retrieve()
                .toBodilessEntity();

        beerPagedListMono.publishOn(Schedulers.parallel()).subscribe(voidResponseEntity -> {
            assertThat(voidResponseEntity.getStatusCode().is2xxSuccessful());
            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testSaveBeerBadRequest() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        BeerDto beerDto = BeerDto.builder()
                .price(new BigDecimal("8.99"))
                .build();

        Mono<ResponseEntity<Void>> beerPagedListMono = webClient.post()
                .uri("/api/v1/beer")
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(beerDto))
                .retrieve()
                .toBodilessEntity();

        beerPagedListMono.publishOn(Schedulers.parallel()).doOnError(throwable -> {
            countDownLatch.countDown();
        }).subscribe(voidResponseEntity -> {

        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testUpdateBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(3);

        webClient.get().uri("/api/v1/beer")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerPagedList.class)
                .publishOn(Schedulers.single())
                .subscribe(pagedList -> {
                    countDownLatch.countDown();

                    //Get existing beer
                    BeerDto beerDto = pagedList.getContent().get(0);

                    BeerDto updatePayload = BeerDto.builder()
                            .beerName("SLsUpdate")
                            .beerStyle(beerDto.getBeerStyle())
                            .upc(beerDto.getUpc())
                            .price(beerDto.getPrice())
                            .build();

                    //updating existing beer
                    webClient.put().uri("/api/v1/beer/" + beerDto.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(updatePayload))
                            .retrieve().toBodilessEntity()
                            .flatMap(responseEntity -> {
                                //get and verify update
                                countDownLatch.countDown();
                                return webClient.get().uri("/api/v1/beer/" + beerDto.getId())
                                        .accept(MediaType.APPLICATION_JSON)
                                        .retrieve()
                                        .bodyToMono(BeerDto.class);
                            }).subscribe(savedDto -> {
                                assertThat(savedDto.getBeerName()).isEqualTo("SLsUpdate");
                                countDownLatch.countDown();
                            });
                });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testUpdateBeerNotFound() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(2);


                    BeerDto updatePayload = BeerDto.builder()
                            .beerName("SLsUpdate")
                            .beerStyle("PALE_ALE")
                            .upc("12345667")
                            .price(new BigDecimal("9.99"))
                            .build();

                    //updating existing beer
                    webClient.put().uri("/api/v1/beer/" + 200)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(updatePayload))
                            .retrieve().toBodilessEntity()
                            .subscribe(savedDto -> {},
                                throwable -> {
                                    if (throwable.getClass().getName().equals("org.springframework.web.reactive.function.client.WebClientResponseException$NotFound")) {
                                        WebClientResponseException ex = (WebClientResponseException) throwable;
                                        if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                                            countDownLatch.countDown();
                                        }
                                    }
                                });

                    countDownLatch.countDown();
                    countDownLatch.await(1000, TimeUnit.MILLISECONDS);
                    assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testDeleteBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(3);

        webClient.get().uri("/api/v1/beer")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerPagedList.class)
                .publishOn(Schedulers.single())
                .subscribe(pagedList -> {
                    countDownLatch.countDown();

                    //Get existing beer
                    BeerDto beerDto = pagedList.getContent().get(0);

                    //deleting existing beer
                    webClient.delete().uri("/api/v1/beer/" + beerDto.getId())
                            .retrieve().toBodilessEntity()
                            .flatMap(responseEntity -> {
                                //get and verify update
                                countDownLatch.countDown();
                                return webClient.get().uri("/api/v1/beer/" + beerDto.getId())
                                        .accept(MediaType.APPLICATION_JSON)
                                        .retrieve()
                                        .bodyToMono(BeerDto.class);
                            }).subscribe(savedDto -> {}, throwable -> {
                                countDownLatch.countDown();
                            });
                });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

}
