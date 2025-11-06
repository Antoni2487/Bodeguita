package io.bootify.my_tiendita.categoria;

import io.bootify.my_tiendita.util.CustomCollectors;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(final CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public Map<Long, String> getCategoriaValues() {
        return categoriaRepository.findAll(Sort.by("id"))
                .stream()
                .collect(CustomCollectors.toSortedMap(Categoria::getId, Categoria::getNombre));
    }

}
