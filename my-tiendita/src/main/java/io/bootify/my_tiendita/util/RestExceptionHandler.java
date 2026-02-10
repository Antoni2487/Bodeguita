package io.bootify.my_tiendita.util;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * Manejador global de excepciones REST
 */
@RestControllerAdvice
public class RestExceptionHandler {

    /**
     * Maneja NotFoundException (404)
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(final NotFoundException exception) {
        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setHttpStatus(HttpStatus.NOT_FOUND.value());
        errorResponse.setException(NotFoundException.class.getSimpleName());
        errorResponse.setMessage(exception.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja ReferencedException (409)
     */
    @ExceptionHandler(ReferencedException.class)
    public ResponseEntity<ErrorResponse> handleReferencedException(final ReferencedException exception) {
        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setHttpStatus(HttpStatus.CONFLICT.value());
        errorResponse.setException(ReferencedException.class.getSimpleName());
        errorResponse.setMessage(exception.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Maneja errores de validación de Bean Validation (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException exception) {
        final BindingResult bindingResult = exception.getBindingResult();
        final List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setHttpStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setException(MethodArgumentNotValidException.class.getSimpleName());
        errorResponse.setFieldErrors(fieldErrors.stream()
                .map(error -> new FieldErrorDTO(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList()));
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    /**
     * Maneja errores de argumentos inválidos (Lógica de Negocio básica)
     * Ej: Stock insuficiente, Fechas incoherentes, etc.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException exception) {
        final ErrorResponse errorResponse = new ErrorResponse();
        // 400 Bad Request es el estatus correcto para "El cliente pidió algo imposible"
        errorResponse.setHttpStatus(HttpStatus.BAD_REQUEST.value()); 
        errorResponse.setException(IllegalArgumentException.class.getSimpleName());
        
        // IMPORTANTE: Aquí pasamos el mensaje que escribimos en el Service
        errorResponse.setMessage(exception.getMessage()); 
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja errores de validación de constraints
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            final ConstraintViolationException exception) {
        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setHttpStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setException(ConstraintViolationException.class.getSimpleName());
        errorResponse.setFieldErrors(exception.getConstraintViolations().stream()
                .map(violation -> new FieldErrorDTO(
                        getPropertyName(violation.getPropertyPath().toString()),
                        violation.getMessage()))
                .collect(Collectors.toList()));
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja errores de integridad de datos (claves duplicadas, violación de constraints)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            final DataIntegrityViolationException exception) {
        
        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setHttpStatus(HttpStatus.CONFLICT.value());
        errorResponse.setException(DataIntegrityViolationException.class.getSimpleName());
        
        String message = "Error de integridad de datos";
        String rootCauseMessage = exception.getRootCause() != null 
            ? exception.getRootCause().getMessage() 
            : exception.getMessage();
        
        // Detectar tipo de error específico
        if (rootCauseMessage != null) {
            if (rootCauseMessage.contains("Duplicate entry")) {
                // Detectar qué campo tiene el duplicado
                if (rootCauseMessage.contains("UK_email") || 
                    rootCauseMessage.toLowerCase().contains("'email'")) {
                    message = "El email ya está registrado en el sistema";
                    errorResponse.addFieldError("email", "Este email ya está en uso");
                    
                } else if (rootCauseMessage.contains("UK_numeroDocumento") || 
                           rootCauseMessage.contains("UKr8iybs7gfuarh9nyn274v6vjk")) {
                    message = "El número de documento ya está registrado en el sistema";
                    errorResponse.addFieldError("numeroDocumento", "Este número de documento ya está en uso");
                    
                } else {
                    message = "Ya existe un registro con estos datos";
                }
            } else if (rootCauseMessage.contains("foreign key constraint") || 
                       rootCauseMessage.contains("FOREIGN KEY")) {
                message = "No se puede eliminar: existen registros relacionados";
            } else if (rootCauseMessage.contains("cannot be null") || 
                       rootCauseMessage.contains("NOT NULL")) {
                message = "Faltan campos obligatorios";
            }
        }
        
        errorResponse.setMessage(message);
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Maneja excepciones genéricas no capturadas
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(final Exception exception) {
        final ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setException(Exception.class.getSimpleName());
        errorResponse.setMessage("Ha ocurrido un error inesperado");
        
        // En desarrollo puedes descomentar esto para ver el error completo
        // errorResponse.setMessage(exception.getMessage());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Extrae el nombre de la propiedad del path completo
     */
    private String getPropertyName(final String propertyPath) {
        final String[] parts = propertyPath.split("\\.");
        return parts[parts.length - 1];
    }

    /**
     * Clase interna para la respuesta de error
     */
    public static class ErrorResponse {
        private Integer httpStatus;
        private String exception;
        private String message;
        private List<FieldErrorDTO> fieldErrors;

        public Integer getHttpStatus() {
            return httpStatus;
        }

        public void setHttpStatus(Integer httpStatus) {
            this.httpStatus = httpStatus;
        }

        public String getException() {
            return exception;
        }

        public void setException(String exception) {
            this.exception = exception;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<FieldErrorDTO> getFieldErrors() {
            return fieldErrors;
        }

        public void setFieldErrors(List<FieldErrorDTO> fieldErrors) {
            this.fieldErrors = fieldErrors;
        }

        public void addFieldError(String field, String message) {
            if (this.fieldErrors == null) {
                this.fieldErrors = new java.util.ArrayList<>();
            }
            this.fieldErrors.add(new FieldErrorDTO(field, message));
        }
    }

    /**
     * DTO para errores de campo
     */
    public static class FieldErrorDTO {
        private String field;
        private String message;

        public FieldErrorDTO(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}