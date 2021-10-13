package guru.springframework.sfgrestbrewery.web.controller;

import guru.springframework.sfgrestbrewery.bootstrap.BeerLoader;
import guru.springframework.sfgrestbrewery.web.functional.BeerRouterConfiguration;
import guru.springframework.sfgrestbrewery.web.model.BeerDto;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by jt on 3/7/21.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class WebClientV2IT {

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

        Mono<BeerDto> beerDtoMono = webClient.get().uri(BeerRouterConfiguration.BEER_V2_URL + "/" + 1)
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
    void getBeerByIdNotFound() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get().uri(BeerRouterConfiguration.BEER_V2_URL + "/" + 1422)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beerDto -> {

        }, throwable -> {
            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void getBeerByUPC() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get().uri(BeerRouterConfiguration.BEER_V2_UPC_URL + "/" + BeerLoader.BEER_2_UPC)
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
    void getBeerByUPCNotFound() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Mono<BeerDto> beerDtoMono = webClient.get().uri(BeerRouterConfiguration.BEER_V2_UPC_URL + "/" + 12345667)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerDto.class);

        beerDtoMono.subscribe(beerDto -> {

        }, throwable -> {
            countDownLatch.countDown();
        });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testSaveBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        BeerDto beerDto = BeerDto.builder()
                .beerName("Sls Beer")
                .beerStyle("PALE_ALE")
                .upc("1233455")
                .price(new BigDecimal("8.99"))
                .build();

        Mono<ResponseEntity<Void>> beerResponseMono = webClient.post().uri(BeerRouterConfiguration.BEER_V2_URL)
                .accept(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(beerDto))
                .retrieve().toBodilessEntity();

        beerResponseMono.publishOn(Schedulers.parallel()).subscribe(responseEntity -> {
            assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isEqualTo(true);
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

        Mono<ResponseEntity<Void>> beerResponseMono = webClient.post().uri(BeerRouterConfiguration.BEER_V2_URL)
                .accept(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(beerDto))
                .retrieve().toBodilessEntity();

        beerResponseMono.subscribe(responseEntity -> {

        }, throwable -> {
            if (throwable.getClass().getName().equals("org.springframework.web.reactive.function.client.WebClientResponseException$BadRequest")){
                WebClientResponseException ex = (WebClientResponseException) throwable;

                if (ex.getStatusCode().equals(HttpStatus.BAD_REQUEST)){
                    countDownLatch.countDown();
                }
            }
        });

        countDownLatch.await(2000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testUpdateBeer() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        final String newBeerName = "Sls Beer";
        final Integer beerId = 1;
        final BeerDto updatedBeerDto =
                BeerDto.builder()
                        .beerName(newBeerName)
                        .beerStyle("PALE_ALE")
                        .upc("12334455")
                        .price(new BigDecimal("8.99"))
                        .build();


        //updating existing beer
        webClient.put().uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(updatedBeerDto))
                .retrieve().toBodilessEntity()
                .subscribe(responseEntity -> {
                    assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isEqualTo(true);
                    countDownLatch.countDown();
                });

        countDownLatch.await(500, TimeUnit.MILLISECONDS);

        webClient.get().uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(BeerDto.class)
                .subscribe(beerDto -> {
                    assertThat(beerDto).isNotNull();
                    assertThat(beerDto.getBeerName()).isNotNull();
                    assertThat(beerDto.getBeerName()).isEqualTo(newBeerName);
                    countDownLatch.countDown();
                });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testUpdateBeerNotFound() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final String newBeerName = "Sls Beer";
        final Integer beerId = 10002;
        final BeerDto updatedBeerDto =
                BeerDto.builder()
                        .beerName(newBeerName)
                        .beerStyle("PALE_ALE")
                        .upc("12334455")
                        .price(new BigDecimal("8.99"))
                        .build();


        //updating existing beer
        webClient.put().uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(updatedBeerDto))
                .retrieve().toBodilessEntity()
                .subscribe(responseEntity -> {
                    assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isEqualTo(true);
                }, throwable -> {
                    countDownLatch.countDown();
                });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testDeleteBeer() throws InterruptedException {
        final Integer beerId = 3;
        CountDownLatch countDownLatch = new CountDownLatch(1);

        webClient.delete().uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                .retrieve().toBodilessEntity()
                .flatMap(responseEntity -> {
                    countDownLatch.countDown();
                    return webClient.get().uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve().bodyToMono(BeerDto.class);
                }).subscribe(deletedDto -> {

                }, throwable -> {
                    countDownLatch.countDown();
                });

        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @Test
    void testDeleteBeerNotFound() {
        final Integer beerId = 4;

        webClient.delete().uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                .retrieve().toBodilessEntity().block();

        assertThrows(WebClientResponseException.NotFound.class, () -> {
            webClient.delete().uri(BeerRouterConfiguration.BEER_V2_URL + "/" + beerId)
                    .retrieve().toBodilessEntity().block();
        });
    }

}
