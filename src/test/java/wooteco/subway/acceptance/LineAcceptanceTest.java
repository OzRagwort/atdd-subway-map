package wooteco.subway.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import wooteco.subway.domain.Station;
import wooteco.subway.dto.LineRequest;
import wooteco.subway.dto.LineResponse;
import wooteco.subway.dto.SectionRequest;

public class LineAcceptanceTest extends AcceptanceTest {

    @Sql(value = "/sql/InsertTwoStation.sql")
    @DisplayName("지하철 노선을 생성한다.")
    @Test
    void createLine() {
        // given
        String paramName = "2호선";
        String paramColor = "초록색";
        Long paramUpStationId = 1L;
        Long paramDownStationId = 2L;
        int paramDistance = 10;
        LineRequest params =
                new LineRequest(paramName, paramColor, paramUpStationId, paramDownStationId, paramDistance);

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();

        // then
        List<Station> stations = response.body().jsonPath().getList("stations", Station.class);
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value()),
                () -> assertThat(response.header("Location")).isNotBlank(),
                () -> assertThat(stations).hasSize(2),
                () -> assertThat(stations.get(0).getId()).isEqualTo(1L),
                () -> assertThat(stations.get(1).getId()).isEqualTo(2L)
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @ParameterizedTest(name = "{displayName} : {arguments}")
    @ValueSource(ints = {1, 255})
    @DisplayName("지하철 노선 이름의 길이를 1 이상 255 이하로 생성할 수 있다.")
    void createLineWithValidName(int count) {
        // given
        String paramName = "a".repeat(count);
        String paramColor = "초록색";
        Long paramUpStationId = 1L;
        Long paramDownStationId = 2L;
        int paramDistance = 10;
        LineRequest params = new LineRequest(paramName, paramColor, paramUpStationId, paramDownStationId,
                paramDistance);

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();

        // then
        List<Station> stations = response.body().jsonPath().getList("stations", Station.class);
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value()),
                () -> assertThat(response.header("Location")).isNotBlank(),
                () -> assertThat(stations).hasSize(2),
                () -> assertThat(stations.get(0).getId()).isEqualTo(1L),
                () -> assertThat(stations.get(1).getId()).isEqualTo(2L)
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @ParameterizedTest(name = "{displayName} : {arguments}")
    @ValueSource(ints = {0, 256})
    @DisplayName("지하철 노선 이름의 길이를 1 이상 255 이하가 아니면 생성할 수 없다.")
    void createLineWithNonValidName(int count) {
        // given
        String paramName = "a".repeat(count);
        String paramColor = "초록색";
        Long paramUpStationId = 1L;
        Long paramDownStationId = 2L;
        int paramDistance = 10;
        LineRequest params = new LineRequest(paramName, paramColor, paramUpStationId, paramDownStationId,
                paramDistance);

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("노선 이름의 길이는 1 이상 255 이하여야 합니다.")
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @ParameterizedTest(name = "{displayName} : {arguments}")
    @ValueSource(strings = {"", "123456789012345678901"})
    @DisplayName("지하철 노선 색의 길이를 1 이상 20 이하가 아니면 생성할 수 없다.")
    void createLineWithNonValidColor(String color) {
        // given
        String paramName = "2호선";
        String paramColor = color;
        Long paramUpStationId = 1L;
        Long paramDownStationId = 2L;
        int paramDistance = 10;
        LineRequest params = new LineRequest(paramName, paramColor, paramUpStationId, paramDownStationId,
                paramDistance);

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("노선 색의 길이는 1 이상 20 이하여야 합니다.")
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @DisplayName("기존에 존재하는 노선 이름으로 지하철 노선을 생성한다.")
    @Test
    void createLineWithDuplicateName() {
        // given
        String paramName = "2호선";
        String paramColor = "초록색";
        Long paramUpStationId = 1L;
        Long paramDownStationId = 2L;
        int paramDistance = 10;
        LineRequest params1 =
                new LineRequest(paramName, paramColor, paramUpStationId, paramDownStationId, paramDistance);
        RestAssured.given().log().all()
                .body(params1)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();

        // when
        paramName = "2호선";
        paramColor = "빨간색";
        paramUpStationId = 1L;
        paramDownStationId = 2L;
        paramDistance = 10;
        LineRequest params2 =
                new LineRequest(paramName, paramColor, paramUpStationId, paramDownStationId, paramDistance);
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params2)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then()
                .log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("같은 이름 혹은 색깔을 가진 노선이 이미 있습니다.")
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @DisplayName("기존에 존재하는 노선 색깔로 지하철 노선을 생성한다.")
    @Test
    void createLineWithDuplicateColor() {
        // given
        String paramName1 = "2호선";
        String paramColor1 = "초록색";
        Long paramUpStationId1 = 1L;
        Long paramDownStationId1 = 2L;
        int paramDistance1 = 10;
        LineRequest params1 =
                new LineRequest(paramName1, paramColor1, paramUpStationId1, paramDownStationId1, paramDistance1);
        RestAssured.given().log().all()
                .body(params1)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();

        // when
        String paramName2 = "3호선";
        String paramColor2 = "초록색";
        Long paramUpStationId2 = 1L;
        Long paramDownStationId2 = 2L;
        int paramDistance2 = 10;
        LineRequest params2 =
                new LineRequest(paramName2, paramColor2, paramUpStationId2, paramDownStationId2, paramDistance2);
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params2)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then()
                .log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("같은 이름 혹은 색깔을 가진 노선이 이미 있습니다.")
        );
    }

    @DisplayName("상하행 역이 없으면 지하철 노선을 생성할 수 없다.")
    @Test
    void createLineNotHasLine() {
        // given
        String paramName = "2호선";
        String paramColor = "초록색";
        Long paramUpStationId = 1L;
        Long paramDownStationId = 2L;
        int paramDistance = 10;
        LineRequest params =
                new LineRequest(paramName, paramColor, paramUpStationId, paramDownStationId, paramDistance);

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("존재하지 않는 역입니다")
        );
    }

    @Sql(value = "/sql/InsertSections.sql")
    @DisplayName("상행 종점 구간을 등록한다.")
    @Test
    void addSectionUpStation() {
        /*
        이미 등록된 노선 아이디 : 1
        이미 등록된 역 아이디 : 1, 2, 3, 4
        구간 등록된 역 아이디 : 1, 2
        역 사이 거리 : 10
         */
        // given
        Long lineId = 1L;
        Long paramUpStationId = 3L;
        Long paramDownStationId = 1L;
        int paramDistance = 10;
        SectionRequest params = new SectionRequest(paramUpStationId, paramDownStationId, paramDistance);

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines/" + lineId + "/sections")
                .then().log().all()
                .extract();

        // then
        ExtractableResponse<Response> findResponse = RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/lines/" + lineId)
                .then().log().all()
                .extract();
        List<Station> stations = findResponse.body().jsonPath().getList("stations", Station.class);
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(stations).hasSize(3),
                () -> assertThat(stations.get(0).getId()).isEqualTo(3L),
                () -> assertThat(stations.get(1).getId()).isEqualTo(1L),
                () -> assertThat(stations.get(2).getId()).isEqualTo(2L)
        );
    }

    @Sql(value = "/sql/InsertSections.sql")
    @DisplayName("하행 종점 구간을 등록한다.")
    @Test
    void addSectionDownStation() {
        /*
        이미 등록된 노선 아이디 : 1
        이미 등록된 역 아이디 : 1, 2, 3, 4
        구간 등록된 역 아이디 : 1, 2
        역 사이 거리 : 10
         */
        // given
        Long lineId = 1L;
        Long paramUpStationId = 2L;
        Long paramDownStationId = 3L;
        int paramDistance = 10;
        SectionRequest params = new SectionRequest(paramUpStationId, paramDownStationId, paramDistance);

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines/" + lineId + "/sections")
                .then().log().all()
                .extract();

        // then
        ExtractableResponse<Response> findResponse = RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/lines/" + lineId)
                .then().log().all()
                .extract();
        List<Station> stations = findResponse.body().jsonPath().getList("stations", Station.class);
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(stations).hasSize(3),
                () -> assertThat(stations.get(0).getId()).isEqualTo(1L),
                () -> assertThat(stations.get(1).getId()).isEqualTo(2L),
                () -> assertThat(stations.get(2).getId()).isEqualTo(3L)
        );
    }

    @Sql(value = "/sql/InsertSections.sql")
    @DisplayName("갈래길 방지로 구간을 등록한다.")
    @Test
    void addSectionBetweenStation() {
        /*
        이미 등록된 노선 아이디 : 1
        이미 등록된 역 아이디 : 1, 2, 3, 4
        구간 등록된 역 아이디 : 1, 2
        역 사이 거리 : 10
         */
        // given
        Long lineId = 1L;
        Long paramUpStationId = 1L;
        Long paramDownStationId = 3L;
        int paramDistance = 5;
        SectionRequest params = new SectionRequest(paramUpStationId, paramDownStationId, paramDistance);

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines/" + lineId + "/sections")
                .then().log().all()
                .extract();

        // then
        ExtractableResponse<Response> findResponse = RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/lines/" + lineId)
                .then().log().all()
                .extract();
        List<Station> stations = findResponse.body().jsonPath().getList("stations", Station.class);
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(stations).hasSize(3),
                () -> assertThat(stations.get(0).getId()).isEqualTo(1L),
                () -> assertThat(stations.get(1).getId()).isEqualTo(3L),
                () -> assertThat(stations.get(2).getId()).isEqualTo(2L)
        );
    }

    @Sql(value = "/sql/InsertSections.sql")
    @DisplayName("기존 상하행 거리보다 거리가 멀거나 같은 경우 구간을 등록할 수 없다.")
    @Test
    void addSectionBetweenStationFailOverDistance() {
        /*
        이미 등록된 노선 아이디 : 1
        이미 등록된 역 아이디 : 1, 2, 3, 4
        구간 등록된 역 아이디 : 1, 2
        역 사이 거리 : 10
         */
        // given
        Long lineId = 1L;
        Long paramUpStationId = 1L;
        Long paramDownStationId = 3L;
        int paramDistance = 10;
        SectionRequest params = new SectionRequest(paramUpStationId, paramDownStationId, paramDistance);

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines/" + lineId + "/sections")
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("역 사이 새로운 역을 등록할 경우 기존 역 사이 길이보다 크거나 같으면 등록을 할 수 없음")
        );
    }

    @Sql(value = "/sql/InsertSections.sql")
    @DisplayName("상행역과 하행역이 이미 노선에 모두 등록되어 있다면 추가할 수 없음")
    @Test
    void addSectionBetweenStationFailAlreadyRegisteredUpAndDown() {
        /*
        이미 등록된 노선 아이디 : 1
        이미 등록된 역 아이디 : 1, 2, 3, 4
        구간 등록된 역 아이디 : 1, 2
        역 사이 거리 : 10
         */
        // given
        Long lineId = 1L;
        Long paramUpStationId = 1L;
        Long paramDownStationId = 2L;
        int paramDistance = 3;
        SectionRequest params = new SectionRequest(paramUpStationId, paramDownStationId, paramDistance);

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines/" + lineId + "/sections")
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("상행역과 하행역이 이미 노선에 모두 등록되어 있다면 추가할 수 없음")
        );
    }

    @Sql(value = "/sql/InsertSections.sql")
    @DisplayName("상행역과 하행역 둘 중 하나도 포함되어있지 않으면 추가할 수 없음")
    @Test
    void addSectionBetweenStationFailNotRegisteredUpAndDown() {
        /*
        이미 등록된 노선 아이디 : 1
        이미 등록된 역 아이디 : 1, 2, 3, 4
        구간 등록된 역 아이디 : 1, 2
        역 사이 거리 : 10
         */
        // given
        Long lineId = 1L;
        Long paramUpStationId = 3L;
        Long paramDownStationId = 3L;
        int paramDistance = 3;
        SectionRequest params = new SectionRequest(paramUpStationId, paramDownStationId, paramDistance);

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines/" + lineId + "/sections")
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("상행역과 하행역 둘 중 하나도 포함되어있지 않으면 추가할 수 없음")
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @DisplayName("지하철노선 목록을 조회한다.")
    @Test
    void getLines() {
        /// given
        String paramName1 = "2호선";
        String paramColor1 = "초록색";
        Long paramUpStationId1 = 1L;
        Long paramDownStationId1 = 2L;
        int paramDistance1 = 10;
        LineRequest params1 =
                new LineRequest(paramName1, paramColor1, paramUpStationId1, paramDownStationId1, paramDistance1);
        ExtractableResponse<Response> createResponse1 = RestAssured.given().log().all()
                .body(params1)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();

        String paramName2 = "5호선";
        String paramColor2 = "보라색";
        Long paramUpStationId2 = 1L;
        Long paramDownStationId2 = 2L;
        int paramDistance2 = 10;
        LineRequest params2 =
                new LineRequest(paramName2, paramColor2, paramUpStationId2, paramDownStationId2, paramDistance2);
        ExtractableResponse<Response> createResponse2 = RestAssured.given().log().all()
                .body(params2)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when()
                .get("/lines")
                .then().log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<Long> expectedLineIds = Arrays.asList(createResponse1, createResponse2).stream()
                .map(it -> Long.parseLong(it.header("Location").split("/")[2]))
                .collect(Collectors.toList());
        List<Long> resultLineIds = response.jsonPath().getList(".", LineResponse.class).stream()
                .map(LineResponse::getId)
                .collect(Collectors.toList());
        assertThat(resultLineIds).containsAll(expectedLineIds);
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @DisplayName("단건의 지하철 노선을 조회한다.")
    @Test
    void getLine() {
        /// given
        String paramName1 = "2호선";
        String paramColor1 = "초록색";
        Long paramUpStationId1 = 1L;
        Long paramDownStationId1 = 2L;
        int paramDistance1 = 10;
        LineRequest params1 =
                new LineRequest(paramName1, paramColor1, paramUpStationId1, paramDownStationId1, paramDistance1);
        ExtractableResponse<Response> createResponse = RestAssured.given().log().all()
                .body(params1)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();
        long createdId = createResponse.body().jsonPath().getLong("id");
        String uri = createResponse.header("Location");

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when()
                .get(uri)
                .then().log().all()
                .extract();

        // then
        List<Station> stations = response.body().jsonPath().getList("stations", Station.class);
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.body().jsonPath().getLong("id")).isEqualTo(createdId),
                () -> assertThat(response.body().jsonPath().getString("name")).isEqualTo("2호선"),
                () -> assertThat(response.body().jsonPath().getString("color")).isEqualTo("초록색"),
                () -> assertThat(stations).hasSize(2),
                () -> assertThat(stations.get(0).getId()).isEqualTo(1L),
                () -> assertThat(stations.get(1).getId()).isEqualTo(2L)
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @DisplayName("존재하지 않는 노선을 조회한다.")
    @Test
    void getNonExistLine() {
        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .when()
                .get("/lines/1")
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("존재하지 않는 노선입니다")
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @DisplayName("노선 정보를 수정한다.")
    @Test
    void updateLine() {
        // given
        String paramName = "2호선";
        String paramColor = "초록색";
        Long paramUpStationId = 1L;
        Long paramDownStationId = 2L;
        int paramDistance = 10;
        LineRequest params = new LineRequest(paramName, paramColor, paramUpStationId, paramDownStationId,
                paramDistance);
        ExtractableResponse<Response> createResponse = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();
        String uri = createResponse.header("Location");

        // when
        String paramName2 = "1호선";
        String paramColor2 = "파란색";
        Long paramUpStationId2 = 1L;
        Long paramDownStationId2 = 2L;
        int paramDistance2 = 10;
        LineRequest updateParam =
                new LineRequest(paramName2, paramColor2, paramUpStationId2, paramDownStationId2, paramDistance2);
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(updateParam)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .put(uri)
                .then().log().all()
                .extract();

        // then
        ExtractableResponse<Response> findLineResponse = RestAssured.given().log().all()
                .body(updateParam)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get(uri)
                .then().log().all()
                .extract();
        LineResponse findLine = findLineResponse.body().jsonPath().getObject(".", LineResponse.class);
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(findLine.getName()).isEqualTo("1호선"),
                () -> assertThat(findLine.getColor()).isEqualTo("파란색")
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @ParameterizedTest(name = "{displayName} : {arguments}")
    @ValueSource(ints = {0, 256})
    @DisplayName("지하철 노선 이름의 길이를 1 이상 255 이하가 아니면 수정할 수 없다.")
    void updateLineNameFail(int count) {
        // given
        String paramName = "2호선";
        String paramColor = "초록색";
        Long paramUpStationId = 1L;
        Long paramDownStationId = 2L;
        int paramDistance = 10;
        LineRequest params = new LineRequest(paramName, paramColor, paramUpStationId, paramDownStationId,
                paramDistance);
        ExtractableResponse<Response> createResponse = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();
        String uri = createResponse.header("Location");

        // when
        String paramName2 = "a".repeat(count);
        String paramColor2 = "파란색";
        Long paramUpStationId2 = 1L;
        Long paramDownStationId2 = 2L;
        int paramDistance2 = 10;
        LineRequest updateParam =
                new LineRequest(paramName2, paramColor2, paramUpStationId2, paramDownStationId2, paramDistance2);
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(updateParam)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .put(uri)
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("노선 이름의 길이는 1 이상 255 이하여야 합니다.")
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @ParameterizedTest(name = "{displayName} : {arguments}")
    @ValueSource(strings = {"", "123456789012345678901"})
    @DisplayName("지하철 노선 색의 길이를 1 이상 20 이하가 아니면 수정할 수 없다.")
    void updateLineColorFail(String color) {
        // given
        String paramName = "2호선";
        String paramColor = "초록색";
        Long paramUpStationId = 1L;
        Long paramDownStationId = 2L;
        int paramDistance = 10;
        LineRequest params = new LineRequest(paramName, paramColor, paramUpStationId, paramDownStationId,
                paramDistance);
        ExtractableResponse<Response> createResponse = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();
        String uri = createResponse.header("Location");

        // when
        String paramName2 = "2호선";
        String paramColor2 = color;
        Long paramUpStationId2 = 1L;
        Long paramDownStationId2 = 2L;
        int paramDistance2 = 10;
        LineRequest updateParam =
                new LineRequest(paramName2, paramColor2, paramUpStationId2, paramDownStationId2, paramDistance2);
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(updateParam)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .put(uri)
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("노선 색의 길이는 1 이상 20 이하여야 합니다.")
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @DisplayName("기존에 존재하는 노선 이름으로 지하철 노선을 수정한다.")
    @Test
    void updateLineWithDuplicateName() {
        // given
        String paramName1 = "1호선";
        String paramColor1 = "파란색";
        Long paramUpStationId1 = 1L;
        Long paramDownStationId1 = 2L;
        int paramDistance1 = 10;
        LineRequest params1 =
                new LineRequest(paramName1, paramColor1, paramUpStationId1, paramDownStationId1, paramDistance1);
        RestAssured.given().log().all()
                .body(params1)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();
        String paramName2 = "2호선";
        String paramColor2 = "초록색";
        Long paramUpStationId2 = 1L;
        Long paramDownStationId2 = 2L;
        int paramDistance2 = 10;
        LineRequest params2 =
                new LineRequest(paramName2, paramColor2, paramUpStationId2, paramDownStationId2, paramDistance2);
        ExtractableResponse<Response> createResponse = RestAssured.given().log().all()
                .body(params2)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();
        String uri = createResponse.header("Location");

        // when
        String paramName3 = "1호선";
        String paramColor3 = "초록색";
        Long paramUpStationId3 = 1L;
        Long paramDownStationId3 = 2L;
        int paramDistance3 = 10;
        LineRequest updateParam =
                new LineRequest(paramName3, paramColor3, paramUpStationId3, paramDownStationId3, paramDistance3);
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(updateParam)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .put(uri)
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("같은 이름 혹은 색깔을 가진 노선이 이미 있습니다.")
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @DisplayName("기존에 존재하는 노선 색깔로 지하철 노선을 수정한다.")
    @Test
    void updateLineWithDuplicateColor() {
        // given
        String paramName1 = "1호선";
        String paramColor1 = "파란색";
        Long paramUpStationId1 = 1L;
        Long paramDownStationId1 = 2L;
        int paramDistance1 = 10;
        LineRequest params1 =
                new LineRequest(paramName1, paramColor1, paramUpStationId1, paramDownStationId1, paramDistance1);
        RestAssured.given().log().all()
                .body(params1)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();
        String paramName2 = "2호선";
        String paramColor2 = "초록색";
        Long paramUpStationId2 = 1L;
        Long paramDownStationId2 = 2L;
        int paramDistance2 = 10;
        LineRequest params2 =
                new LineRequest(paramName2, paramColor2, paramUpStationId2, paramDownStationId2, paramDistance2);
        ExtractableResponse<Response> createResponse = RestAssured.given().log().all()
                .body(params2)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();
        String uri = createResponse.header("Location");

        // when
        String paramName3 = "2호선";
        String paramColor3 = "파란색";
        Long paramUpStationId3 = 1L;
        Long paramDownStationId3 = 2L;
        int paramDistance3 = 10;
        LineRequest updateParam =
                new LineRequest(paramName3, paramColor3, paramUpStationId3, paramDownStationId3, paramDistance3);
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(updateParam)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .put(uri)
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("같은 이름 혹은 색깔을 가진 노선이 이미 있습니다.")
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @DisplayName("존재하지 않는 노선을 수정한다.")
    @Test
    void updateNonExistLine() {
        // given
        String paramName = "2호선";
        String paramColor = "초록색";
        Long paramUpStationId = 1L;
        Long paramDownStationId = 2L;
        int paramDistance = 10;
        LineRequest params = new LineRequest(paramName, paramColor, paramUpStationId, paramDownStationId,
                paramDistance);

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .put("/lines/1")
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("존재하지 않는 노선입니다")
        );
    }

    @Sql(value = "/sql/InsertTwoStation.sql")
    @DisplayName("노선을 제거한다")
    @Test
    void deleteById() {
        // given
        String paramName = "2호선";
        String paramColor = "초록색";
        Long paramUpStationId = 1L;
        Long paramDownStationId = 2L;
        int paramDistance = 10;
        LineRequest params = new LineRequest(paramName, paramColor, paramUpStationId, paramDownStationId,
                paramDistance);
        ExtractableResponse<Response> createResponse = RestAssured.given().log().all()
                .body(params)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();
        String uri = createResponse.header("Location");

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .delete(uri)
                .then().log().all()
                .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    @Sql(value = "/sql/InsertTwoSections.sql")
    @DisplayName("종점이 제거될 경우 다음으로 오던 역이 종점이 됨")
    @Test
    void deleteSectionTerminal() {
        /*
        이미 등록된 노선 아이디 : 1
        이미 등록된 역 아이디 : 1, 2, 3, 4
        구간 등록된 역 아이디 : (1, 2), (2, 3)
        역 사이 거리 : 10, 10
         */
        // given
        Long lineId = 1L;
        Long stationId = 3L;

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .delete("/lines/" + lineId + "/sections?stationId=" + stationId)
                .then().log().all()
                .extract();

        // then
        ExtractableResponse<Response> findResponse = RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/lines/" + lineId)
                .then().log().all()
                .extract();
        List<Station> stations = findResponse.body().jsonPath().getList("stations", Station.class);
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(stations).hasSize(2),
                () -> assertThat(stations.get(0).getId()).isEqualTo(1L),
                () -> assertThat(stations.get(1).getId()).isEqualTo(2L)
        );
    }

    @Sql(value = "/sql/InsertTwoSections.sql")
    @DisplayName("중간역이 제거될 경우 재배치를 함")
    @Test
    void deleteSectionWhenFork() {
        /*
        이미 등록된 노선 아이디 : 1
        이미 등록된 역 아이디 : 1, 2, 3, 4
        구간 등록된 역 아이디 : (1, 2), (2, 3)
        역 사이 거리 : 10, 10
         */
        // given
        Long lineId = 1L;
        Long stationId = 2L;

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .delete("/lines/" + lineId + "/sections?stationId=" + stationId)
                .then().log().all()
                .extract();

        // then
        ExtractableResponse<Response> findResponse = RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/lines/" + lineId)
                .then().log().all()
                .extract();
        List<Station> stations = findResponse.body().jsonPath().getList("stations", Station.class);
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(stations).hasSize(2),
                () -> assertThat(stations.get(0).getId()).isEqualTo(1L),
                () -> assertThat(stations.get(1).getId()).isEqualTo(3L)
        );
    }

    @Sql(value = "/sql/InsertSections.sql")
    @DisplayName("구간이 하나인 노선에서 마지막 구간을 제거할 수 없음")
    @Test
    void deleteSectionFailWhenLastSection() {
        /*
        이미 등록된 노선 아이디 : 1
        이미 등록된 역 아이디 : 1, 2, 3, 4
        구간 등록된 역 아이디 : 1, 2
        역 사이 거리 : 10
         */
        // given
        Long lineId = 1L;
        Long stationId = 2L;

        // when
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .delete("/lines/" + lineId + "/sections?stationId=" + stationId)
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value()),
                () -> assertThat(response.body().jsonPath().getString("message"))
                        .isEqualTo("구간이 하나인 노선에서 마지막 구간을 제거할 수 없음")
        );
    }
}
