package cl.duoc.ventas.service;

import cl.duoc.ventas.exception.AlmacenamientoException;
import cl.duoc.ventas.exception.SolicitudInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Almacenamiento temporal en Amazon EFS.
 *
 * Para Java, EFS es simplemente una carpeta del disco: en la EC2 el EFS esta montado en /mnt/efs
 * y Docker lo monta dentro del contenedor en /app/efs (variable EFS_PATH).
 * Por eso aqui se usa java.nio.file.Files y no un SDK de AWS.
 */
@Service
public class AlmacenamientoEfsService {

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoEfsService.class);

    private final Path base;

    public AlmacenamientoEfsService(@Value("${app.efs.path}") String rutaBase) {
        this.base = Paths.get(rutaBase).toAbsolutePath().normalize();
        log.info("Almacenamiento temporal (EFS) en {}", base);
    }

    public Path getBase() {
        return base;
    }

    /** Escribe (o sobrescribe) un archivo, creando las carpetas que falten. */
    public void guardar(String rutaRelativa, byte[] contenido) {
        Path destino = resolver(rutaRelativa);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
            log.info("Archivo guardado en EFS: {} ({} bytes)", destino, contenido.length);
        } catch (IOException e) {
            throw new AlmacenamientoException("No se pudo escribir en EFS (" + destino + "): " + e.getMessage(), e);
        }
    }

    public byte[] leer(String rutaRelativa) {
        Path origen = resolver(rutaRelativa);
        try {
            return Files.readAllBytes(origen);
        } catch (IOException e) {
            throw new AlmacenamientoException("No se pudo leer de EFS (" + origen + "): " + e.getMessage(), e);
        }
    }

    public boolean existe(String rutaRelativa) {
        return Files.exists(resolver(rutaRelativa));
    }

    /** Borra el archivo si existe (no falla si ya no esta). */
    public void eliminar(String rutaRelativa) {
        Path archivo = resolver(rutaRelativa);
        try {
            if (Files.deleteIfExists(archivo)) {
                log.info("Archivo eliminado de EFS: {}", archivo);
            }
        } catch (IOException e) {
            throw new AlmacenamientoException("No se pudo eliminar de EFS (" + archivo + "): " + e.getMessage(), e);
        }
    }

    /** Convierte la ruta relativa en absoluta e impide salir de la carpeta base (ej: "../../etc"). */
    private Path resolver(String rutaRelativa) {
        Path ruta = base.resolve(rutaRelativa).normalize();
        if (!ruta.startsWith(base)) {
            throw new SolicitudInvalidaException("Ruta de archivo invalida: " + rutaRelativa);
        }
        return ruta;
    }
}
