package ua.com.epam.assembler.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import ua.com.epam.assembler.mapper.converter.AuthorToAuthorGetDto;
import ua.com.epam.entity.Author;
import ua.com.epam.entity.dto.author.AuthorGetDto;

@Service
public class ModelToDtoMapper {

    private ModelMapper modelMapper = new ModelMapper();

    public ModelToDtoMapper() {
        modelMapper.addConverter(new AuthorToAuthorGetDto());
    }

    public AuthorGetDto getAuthorGetDtoFromModel(Author author) {
        return modelMapper.map(author, AuthorGetDto.class);
    }
}
