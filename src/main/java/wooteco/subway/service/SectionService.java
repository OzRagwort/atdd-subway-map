package wooteco.subway.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import wooteco.subway.dao.SectionDao;
import wooteco.subway.domain.Section;
import wooteco.subway.domain.Sections;
import wooteco.subway.dto.SectionRequest;
import wooteco.subway.exception.NoSuchSectionException;

@Service
public class SectionService {

    private final SectionDao sectionDao;

    public SectionService(SectionDao sectionDao) {
        this.sectionDao = sectionDao;
    }

    public void addSection(Long lineId, SectionRequest request) {
        Long upStationId = request.getUpStationId();
        Long downStationId = request.getDownStationId();
        int distance = request.getDistance();

        checkHasStation(lineId, upStationId, downStationId);
        updateIfForkLine(lineId, request);

        sectionDao.save(new Section(lineId, upStationId, downStationId, distance));
    }

    private void checkHasStation(Long lineId, Long upStationId, Long downStationId) {
        Sections sections = new Sections(sectionDao.findAllByLineId(lineId));
        sections.checkHasStation(upStationId, downStationId);
    }

    private void updateIfForkLine(Long lineId, SectionRequest request) {
        Optional<Section> sectionByUpStation = sectionDao.findByLineIdAndUpStationId(lineId, request.getUpStationId());
        Optional<Section> sectionByDownStation =
                sectionDao.findByLineIdAndDownStationId(lineId, request.getDownStationId());

        if (sectionByUpStation.isPresent() && sectionByDownStation.isPresent()) {
            throw new IllegalArgumentException("상행역과 하행역이 이미 노선에 모두 등록되어 있다면 추가할 수 없음");
        }

        if (sectionByUpStation.isPresent()) {
            Section section = sectionByUpStation.get();
            int distance = section.getDistance() - request.getDistance();
            validateDistance(distance);

            sectionDao.update(section.getId(), new Section(
                    lineId,
                    request.getDownStationId(),
                    section.getDownStationId(),
                    distance));
            return;
        }
        if (sectionByDownStation.isPresent()) {
            Section section = sectionByDownStation.get();
            int distance = request.getDistance() - section.getDistance();
            validateDistance(distance);

            sectionDao.update(section.getId(), new Section(
                    lineId,
                    section.getUpStationId(),
                    request.getUpStationId(),
                    distance));
        }
    }

    private void validateDistance(int distance) {
        if (distance <= 0) {
            throw new IllegalArgumentException("역 사이 새로운 역을 등록할 경우 기존 역 사이 길이보다 크거나 같으면 등록을 할 수 없음");
        }
    }

    public void deleteStation(Long lineId, Long stationId) {
        Sections sections = new Sections(sectionDao.findAllByLineId(lineId));
        validateDeleteStation(stationId, sections);

        if (sections.isTerminal(stationId)) {
            sectionDao.deleteByLineIdAndStationId(lineId, stationId);
            return;
        }
        deleteStationWhenForkSection(lineId, stationId);
    }

    private void validateDeleteStation(Long stationId, Sections sections) {
        if (!sections.getSortedStationId().contains(stationId)) {
            throw new NoSuchSectionException();
        }
        if (sections.isEmpty() || sections.isMinimumSize()) {
            throw new IllegalArgumentException("구간이 하나인 노선에서 마지막 구간을 제거할 수 없음");
        }
    }

    private void deleteStationWhenForkSection(Long lineId, Long stationId) {
        Optional<Section> findUpStation = sectionDao.findByLineIdAndUpStationId(lineId, stationId);
        Optional<Section> findDownStation = sectionDao.findByLineIdAndDownStationId(lineId, stationId);

        if (findUpStation.isEmpty() || findDownStation.isEmpty()) {
            throw new IllegalStateException("구간 정보가 잘못되었습니다.");
        }

        Section upStation = findUpStation.get();
        Section downStation = findDownStation.get();

        sectionDao.update(downStation.getId(), new Section(
                lineId,
                downStation.getUpStationId(),
                upStation.getDownStationId(),
                upStation.getDistance() + downStation.getDistance()));
        sectionDao.deleteById(upStation.getId());
    }
}
