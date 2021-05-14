package wooteco.subway.service;

import org.springframework.stereotype.Service;
import wooteco.subway.controller.request.LineAndSectionCreateRequest;
import wooteco.subway.dao.LineDao;
import wooteco.subway.domain.Line;
import wooteco.subway.domain.SimpleLine;
import wooteco.subway.exception.line.LineColorDuplicateException;
import wooteco.subway.exception.line.LineNameDuplicateException;
import wooteco.subway.exception.line.LineNotFoundException;
import wooteco.subway.exception.section.BothEndStationsSameException;
import wooteco.subway.service.dto.LineDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LineService {

    private final LineDao lineDao;

    public LineService(LineDao lineDao) {
        this.lineDao = lineDao;
    }

    public Line create(SimpleLine simpleLine) {
        final Long id = lineDao.insert(simpleLine.toLine());
        return lineDao.findById(id);
    }

    public List<LineDto> findAll() {
        List<Line> lines = lineDao.findAll();
        return lines.stream()
                .map(LineDto::new)
                .collect(Collectors.toList());
    }

    public LineDto findById(Long id) {
        if (!lineDao.isExistById(id)) {
            throw new LineNotFoundException();
        }
        final Line line = lineDao.findById(id);
        return new LineDto(line);
    }

    public void checkIfExistsById(Long id) {
        if (!lineDao.isExistById(id)) {
            throw new LineNotFoundException();
        }
    }

    public void updateById(Long id, SimpleLine simpleLine) {
        lineDao.update(id, simpleLine.toLine());
    }

    public void deleteById(Long id) {
        lineDao.delete(id);
    }

    public void validate(LineAndSectionCreateRequest lineAndSectionCreateRequest) {
        validateSameEndStations(lineAndSectionCreateRequest.getUpStationId(),
                lineAndSectionCreateRequest.getDownStationId());
        validateDuplicateName(lineAndSectionCreateRequest.getName());
        validateDuplicateColor(lineAndSectionCreateRequest.getColor());
    }

    private void validateSameEndStations(Long upStationId, Long downStationId) {
        if (upStationId.equals(downStationId)) {
            throw new BothEndStationsSameException();
        }
    }

    private void validateDuplicateName(String name) {
        if (lineDao.isExistByName(name)) {
            throw new LineNameDuplicateException();
        }
    }

    private void validateDuplicateColor(String color) {
        if (lineDao.isExistByColor(color)) {
            throw new LineColorDuplicateException();
        }
    }
}
