package com.planagent.amap;

import com.planagent.model.*;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(AmapClient.class)
@TestPropertySource(properties = {"amap.api-key=test-key"})
class AmapClientTest {

    @Autowired
    private AmapClient amapClient;

    @Autowired
    private MockRestServiceServer server;

    @BeforeEach
    void clearAmapCache() {
        amapClient.clearCache();
    }

    private static Matcher<? super String> uriContains(String substring) {
        return new BaseMatcher<>() {
            @Override
            public boolean matches(Object actual) {
                return actual instanceof String s && s.contains(substring);
            }
            @Override
            public void describeTo(Description desc) {
                desc.appendText("URI containing ").appendValue(substring);
            }
        };
    }

    private void expectGeocode() {
        server.expect(requestTo(uriContains("/v3/geocode/geo")))
            .andRespond(withSuccess(
                "{\"status\":\"1\",\"geocodes\":[{\"adcode\":\"110105\",\"location\":\"116.443,39.921\"}]}",
                MediaType.APPLICATION_JSON));
    }

    private void warmGeocodeCache(String district) {
        amapClient.geocode(district);
    }

    @Test
    void disabledWithoutKey() {
        AmapClient noKeyClient = new AmapClient("", org.springframework.web.client.RestClient.builder());
        assertFalse(noKeyClient.isEnabled());
    }

    @Test
    void enabledWithKey() {
        assertTrue(amapClient.isEnabled());
    }

    @Test
    void checkWeatherSuccess() {
        server.expect(requestTo(uriContains("/v3/geocode/geo")))
            .andRespond(withSuccess("""
                {"status":"1","geocodes":[{"adcode":"110105","location":"116.443,39.921"}]}
                """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(uriContains("/v3/weather/weatherInfo")))
            .andRespond(withSuccess("""
                {"status":"1","lives":[{"weather":"晴","temperature":"28"}]}
                """, MediaType.APPLICATION_JSON));

        WeatherInfo result = amapClient.checkWeather("朝阳区");
        assertNotNull(result);
        assertEquals("朝阳区", result.district);
        assertEquals("晴", result.weather);
        assertEquals("28°C", result.temperature);
        assertTrue(result.suitable);
    }

    @Test
    void checkWeatherRainNotSuitable() {
        server.expect(requestTo(uriContains("/v3/geocode/geo")))
            .andRespond(withSuccess("""
                {"status":"1","geocodes":[{"adcode":"110108","location":"116.298,39.960"}]}
                """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(uriContains("/v3/weather/weatherInfo")))
            .andRespond(withSuccess("""
                {"status":"1","lives":[{"weather":"中雨","temperature":"22"}]}
                """, MediaType.APPLICATION_JSON));

        WeatherInfo result = amapClient.checkWeather("海淀区");
        assertNotNull(result);
        assertEquals("中雨", result.weather);
        assertFalse(result.suitable);
    }

    @Test
    void checkWeatherGeocodeFailureReturnsNull() {
        server.expect(requestTo(uriContains("/v3/geocode/geo")))
            .andRespond(withServerError());

        WeatherInfo result = amapClient.checkWeather("朝阳区");
        assertNull(result);
    }

    @Test
    void checkWeatherApiFailureReturnsNull() {
        server.expect(requestTo(uriContains("/v3/geocode/geo")))
            .andRespond(withSuccess("""
                {"status":"1","geocodes":[{"adcode":"110105","location":"116.443,39.921"}]}
                """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(uriContains("/v3/weather/weatherInfo")))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        WeatherInfo result = amapClient.checkWeather("朝阳区");
        assertNull(result);
    }

    @Test
    void geocodeCaching() {
        // Single geocode call
        server.expect(requestTo(uriContains("/v3/geocode/geo")))
            .andRespond(withSuccess("""
                {"status":"1","geocodes":[{"adcode":"110105","location":"116.443,39.921"}]}
                """, MediaType.APPLICATION_JSON));

        // Two weather calls — second uses cached geocode
        server.expect(requestTo(uriContains("/v3/weather/weatherInfo")))
            .andRespond(withSuccess("""
                {"status":"1","lives":[{"weather":"晴","temperature":"28"}]}
                """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(uriContains("/v3/weather/weatherInfo")))
            .andRespond(withSuccess("""
                {"status":"1","lives":[{"weather":"多云","temperature":"26"}]}
                """, MediaType.APPLICATION_JSON));

        WeatherInfo r1 = amapClient.checkWeather("朝阳区");
        WeatherInfo r2 = amapClient.checkWeather("朝阳区");

        assertNotNull(r1);
        assertEquals("晴", r1.weather);
        assertNotNull(r2);
        assertEquals("多云", r2.weather);

        server.verify();
    }

    @Test
    void searchActivitiesSuccess() {
        expectGeocode();
        server.expect(requestTo(uriContains("/v5/place/around")))
            .andRespond(withSuccess("""
                {"status":"1","pois":[
                    {"id":"B001","name":"欢乐谷","type":"游乐场","typecode":"050301",
                     "location":"116.50,39.87","distance":"3500",
                     "biz_ext":{"rating":"4.5","cost":"200"},
                     "deep_info":{"opentime":"09:00-21:00"}},
                    {"id":"B002","name":"朝阳公园","type":"公园","typecode":"110101",
                     "location":"116.48,39.93","distance":"1800",
                     "biz_ext":{"rating":"4.3"},
                     "deep_info":{"opentime":"06:00-22:00"}}
                ]}
                """, MediaType.APPLICATION_JSON));

        warmGeocodeCache("朝阳区");
        List<Activity> results = amapClient.searchActivities("亲子乐园", 5, "2-3小时", "5.0km");
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("欢乐谷", results.get(0).name);
        assertEquals("3.5km", results.get(0).distance);
        assertEquals(4.5, results.get(0).rating);
        assertEquals("¥200/人", results.get(0).price);
        assertEquals("09:00-21:00", results.get(0).openTime);
    }

    @Test
    void searchRestaurantsSuccess() {
        expectGeocode();
        server.expect(requestTo(uriContains("/v5/place/around")))
            .andRespond(withSuccess("""
                {"status":"1","pois":[
                    {"id":"R001","name":"大董烤鸭","type":"餐饮","typecode":"050101",
                     "location":"116.50,39.87","distance":"1200",
                     "biz_ext":{"rating":"4.7","cost":"150"}}
                ]}
                """, MediaType.APPLICATION_JSON));

        warmGeocodeCache("朝阳区");
        List<Restaurant> results = amapClient.searchRestaurants("烤鸭", null, null, false);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("大董烤鸭", results.get(0).name);
        assertEquals("1.2km", results.get(0).distance);
        assertEquals(4.7, results.get(0).rating);
    }

    @Test
    void planRouteSuccess() {
        // First geocode
        server.expect(requestTo(uriContains("/v3/geocode/geo")))
            .andRespond(withSuccess("""
                {"status":"1","geocodes":[{"adcode":"110101","location":"116.428,39.903"}]}
                """, MediaType.APPLICATION_JSON));

        // Second geocode
        server.expect(requestTo(uriContains("/v3/geocode/geo")))
            .andRespond(withSuccess("""
                {"status":"1","geocodes":[{"adcode":"110101","location":"116.403,39.909"}]}
                """, MediaType.APPLICATION_JSON));

        // Route
        server.expect(requestTo(uriContains("/v3/direction/driving")))
            .andRespond(withSuccess("""
                {"status":"1","route":{"paths":[{"distance":"3500","duration":"900"}]}}
                """, MediaType.APPLICATION_JSON));

        RouteInfo result = amapClient.planRoute("北京站", "天安门", "驾车");
        assertNotNull(result);
        assertEquals("驾车", result.mode);
        assertEquals("3.5km", result.distance);
        assertEquals("15分钟", result.duration);
    }

    @Test
    void planRouteTransit() {
        server.expect(requestTo(uriContains("/v3/geocode/geo")))
            .andRespond(withSuccess("""
                {"status":"1","geocodes":[{"adcode":"110105","location":"116.456,39.932"}]}
                """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(uriContains("/v3/geocode/geo")))
            .andRespond(withSuccess("""
                {"status":"1","geocodes":[{"adcode":"110105","location":"116.460,39.914"}]}
                """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(uriContains("/v3/direction/transit")))
            .andRespond(withSuccess("""
                {"status":"1","route":{"paths":[{"distance":"8500","duration":"2700"}]}}
                """, MediaType.APPLICATION_JSON));

        RouteInfo result = amapClient.planRoute("三里屯", "国贸", "公交");
        assertNotNull(result);
        assertEquals("公交", result.mode);
        assertEquals("8.5km", result.distance);
        assertTrue(result.duration.contains("分钟"));
    }

    @Test
    void planRouteWalking() {
        server.expect(requestTo(uriContains("/v3/geocode/geo")))
            .andRespond(withSuccess("""
                {"status":"1","geocodes":[{"adcode":"110101","location":"116.397,39.917"}]}
                """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(uriContains("/v3/geocode/geo")))
            .andRespond(withSuccess("""
                {"status":"1","geocodes":[{"adcode":"110101","location":"116.389,39.933"}]}
                """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(uriContains("/v3/direction/walking")))
            .andRespond(withSuccess("""
                {"status":"1","route":{"paths":[{"distance":"1800","duration":"1200"}]}}
                """, MediaType.APPLICATION_JSON));

        RouteInfo result = amapClient.planRoute("南锣鼓巷", "后海", "步行");
        assertNotNull(result);
        assertEquals("步行", result.mode);
    }

    @Test
    void filterByRatingSuccess() {
        server.expect(requestTo("https://restapi.amap.com/v3/place/detail?key=test-key&id=B001"))
            .andRespond(withSuccess("""
                {"status":"1","pois":[{"id":"B001","name":"高评分餐厅",
                 "type":"餐饮","biz_ext":{"rating":"4.8"}}]}
                """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://restapi.amap.com/v3/place/detail?key=test-key&id=B002"))
            .andRespond(withSuccess("""
                {"status":"1","pois":[{"id":"B002","name":"低评分餐厅",
                 "type":"餐饮","biz_ext":{"rating":"3.5"}}]}
                """, MediaType.APPLICATION_JSON));

        var results = amapClient.filterByRating(List.of("B001", "B002"), 4.0);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("B001", results.get(0).get("id"));
        assertEquals(4.8, results.get(0).get("rating"));
    }

    @Test
    void filterByRatingAllFailReturnsNull() {
        server.expect(requestTo("https://restapi.amap.com/v3/place/detail?key=test-key&id=B001"))
            .andRespond(withServerError());

        server.expect(requestTo("https://restapi.amap.com/v3/place/detail?key=test-key&id=B002"))
            .andRespond(withServerError());

        var results = amapClient.filterByRating(List.of("B001", "B002"), 4.0);
        assertNull(results);
    }

    @Test
    void searchActivitiesApiFailureReturnsNull() {
        expectGeocode();
        server.expect(requestTo(uriContains("/v5/place/around")))
            .andRespond(withServerError());

        warmGeocodeCache("朝阳区");
        List<Activity> results = amapClient.searchActivities("公园", 5, "2-3小时", "5.0km");
        assertNull(results);
    }

    @Test
    void searchRestaurantsApiFailureReturnsNull() {
        expectGeocode();
        server.expect(requestTo(uriContains("/v5/place/around")))
            .andRespond(withServerError());

        warmGeocodeCache("朝阳区");
        List<Restaurant> results = amapClient.searchRestaurants("粤菜", null, null, false);
        assertNull(results);
    }

    @Test
    void planRouteGeocodeFailureReturnsNull() {
        server.expect(requestTo(uriContains("/v3/geocode/geo")))
            .andRespond(withServerError());

        RouteInfo result = amapClient.planRoute("朝阳区", "海淀区", "驾车");
        assertNull(result);
    }
}
