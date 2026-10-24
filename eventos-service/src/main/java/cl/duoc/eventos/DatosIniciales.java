package cl.duoc.eventos;

import cl.duoc.eventos.model.Evento;
import cl.duoc.eventos.repository.EventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Carga algunos eventos de ejemplo solo si la tabla esta vacia.
 * Se desactiva con la variable de entorno APP_DATOS_INICIALES=false.
 */
@Component
@ConditionalOnProperty(name = "app.datos-iniciales", havingValue = "true", matchIfMissing = true)
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final EventoRepository repository;

    public DatosIniciales(EventoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        LocalDateTime base = LocalDateTime.now().plusMonths(1).withHour(20).withMinute(0).withSecond(0).withNano(0);
        repository.saveAll(List.of(
                new Evento("Concierto Rock en el Parque", "Bandas nacionales en vivo",
                        "Parque O'Higgins, Santiago", base, 25000L, 500),
                new Evento("Festival de Jazz Valparaiso", "Tres escenarios y food trucks",
                        "Muelle Baron, Valparaiso", base.plusWeeks(2), 18000L, 300),
                new Evento("Stand-up Comedy Night", "Show de comedia para mayores de 18",
                        "Teatro Caupolican, Santiago", base.plusWeeks(3).withHour(21), 12000L, 150)
        ));
        log.info("Se cargaron 3 eventos de ejemplo");
    }
}
