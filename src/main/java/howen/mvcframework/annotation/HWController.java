package howen.mvcframework.annotation;

        import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HWController {
    String value() default "";
}
