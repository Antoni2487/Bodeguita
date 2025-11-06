package io.bootify.my_tiendita.pedido;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import org.springframework.web.servlet.HandlerMapping;


/**
 * Validate that the venta value isn't taken yet.
 */
@Target({ FIELD, METHOD, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = PedidoVentaUnique.PedidoVentaUniqueValidator.class
)
public @interface PedidoVentaUnique {

    String message() default "{Exists.pedido.venta}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class PedidoVentaUniqueValidator implements ConstraintValidator<PedidoVentaUnique, Long> {

        private final PedidoService pedidoService;
        private final HttpServletRequest request;

        public PedidoVentaUniqueValidator(final PedidoService pedidoService,
                final HttpServletRequest request) {
            this.pedidoService = pedidoService;
            this.request = request;
        }

        @Override
        public boolean isValid(final Long value, final ConstraintValidatorContext cvContext) {
            if (value == null) {
                // no value present
                return true;
            }
            @SuppressWarnings("unchecked") final Map<String, String> pathVariables =
                    ((Map<String, String>)request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
            final String currentId = pathVariables.get("id");
            if (currentId != null && value.equals(pedidoService.get(Long.parseLong(currentId)).getVenta())) {
                // value hasn't changed
                return true;
            }
            return !pedidoService.ventaExists(value);
        }

    }

}
