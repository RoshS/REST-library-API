package ua.com.epam.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ua.com.epam.entity.Author;
import ua.com.epam.entity.Book;
import ua.com.epam.entity.Genre;
import ua.com.epam.entity.dto.genre.GenreDto;
import ua.com.epam.entity.dto.genre.SimpleGenreWithAuthorsDto;
import ua.com.epam.entity.dto.genre.SimpleGenreWithBooksDto;
import ua.com.epam.entity.exception.IdMismatchException;
import ua.com.epam.entity.exception.genre.GenreAlreadyExistsException;
import ua.com.epam.entity.exception.genre.GenreNameAlreadyExistsException;
import ua.com.epam.entity.exception.genre.GenreNotFoundException;
import ua.com.epam.entity.exception.genre.BooksInGenreArePresentException;
import ua.com.epam.repository.AuthorRepository;
import ua.com.epam.repository.BookRepository;
import ua.com.epam.repository.GenreRepository;
import ua.com.epam.repository.JsonKeysConformity;
import ua.com.epam.service.mapper.DtoToModelMapper;
import ua.com.epam.service.mapper.ModelToDtoMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GenreService {

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ModelToDtoMapper toDtoMapper;

    @Autowired
    private DtoToModelMapper toModelMapper;

    private Sort.Direction getSortDirection(String order) {
        Sort.Direction orderType = null;

        if (order.equals("desc"))
            orderType = Sort.Direction.DESC;
        else if (order.equals("asc"))
            orderType = Sort.Direction.ASC;

        return orderType;
    }

    public GenreDto findGenreByGenreId(long genreId) {
        Optional<Genre> exist = genreRepository.getOneByGenreId(genreId);

        if (!exist.isPresent()) {
            throw new GenreNotFoundException(genreId);
        }

        Genre toGet = exist.get();
        return toDtoMapper.mapGenreToGenreDto(toGet);
    }

    public SimpleGenreWithAuthorsDto findGenreWithItAuthors(long genreId, int authorsCount) {
        Optional<Genre> opt = genreRepository.getOneByGenreId(genreId);

        if (!opt.isPresent()) {
            throw new GenreNotFoundException(genreId);
        }

        Genre genre = opt.get();
        List<Author> authorsInGenre = authorRepository.getAllAuthorsInGenre(genreId)
                .stream()
                .limit(authorsCount)
                .collect(Collectors.toList());

        return toDtoMapper.getSimpleGenreWithAuthorsDto(genre, authorsInGenre);
    }

    public SimpleGenreWithBooksDto findGenreWithItBooksList(long genreId, int booksCount) {
        Optional<Genre> opt = genreRepository.getOneByGenreId(genreId);

        if (!opt.isPresent()) {
            throw new GenreNotFoundException(genreId);
        }

        Genre genre = opt.get();
        List<Book> booksInGenre = bookRepository.getAllBooksInGenre(genreId)
                .stream()
                .limit(booksCount)
                .collect(Collectors.toList());

        return toDtoMapper.getSimpleGenreWithBooksDto(genre, booksInGenre);
    }

    public List<GenreDto> findAllGenres(String sortBy, String order, int page, int size) {
        Sort.Direction orderType = getSortDirection(order);

        String sortParameter = JsonKeysConformity.getPropNameByJsonKey(sortBy);

        return genreRepository.getAllGenresOrdered(Sort.by(orderType, sortParameter))
                .stream()
                .skip((page - 1) * size)
                .limit(size)
                .map(toDtoMapper::mapGenreToGenreDto)
                .collect(Collectors.toList());
    }

    public GenreDto addNewGenre(GenreDto genre) {
        if (genreRepository.existsByGenreId(genre.getGenreId())) {
            throw new GenreAlreadyExistsException();
        }

        if (genreRepository.existsByGenreName(genre.getGenreName())) {
            throw new GenreNameAlreadyExistsException();
        }

        Genre toPost = toModelMapper.mapGenreDtoToGenre(genre);
        Genre response = genreRepository.save(toPost);

        return toDtoMapper.mapGenreToGenreDto(response);
    }

    public GenreDto updateExistedGenre(long genreId, GenreDto genre) {
        Optional<Genre> opt = genreRepository.getOneByGenreId(genreId);

        if (!opt.isPresent()) {
            throw new GenreNotFoundException(genreId);
        }
        if (genreId != genre.getGenreId()) {
            throw new IdMismatchException();
        }

        Genre proxy = opt.get();

        proxy.setGenreId(genre.getGenreId());
        proxy.setGenreName(genre.getGenreName());
        proxy.setGenreDescription(genre.getDescription());

        Genre updated = genreRepository.save(proxy);
        return toDtoMapper.mapGenreToGenreDto(updated);
    }

    public GenreDto deleteExistedGenre(long genreId, boolean forcibly) {
        Optional<Genre> opt = genreRepository.getOneByGenreId(genreId);

        if (!opt.isPresent()) {
            throw new GenreNotFoundException(genreId);
        }

        Genre toDelete = opt.get();
        long booksCount = bookRepository.getAllBooksInGenre(genreId).size();

        if (booksCount > 0 && !forcibly) {
            throw new BooksInGenreArePresentException(genreId, booksCount);
        }

        genreRepository.delete(toDelete);
        return toDtoMapper.mapGenreToGenreDto(toDelete);
    }
}
