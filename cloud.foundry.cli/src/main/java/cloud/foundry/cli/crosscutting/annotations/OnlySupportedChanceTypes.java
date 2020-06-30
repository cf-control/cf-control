//package cloud.foundry.cli.crosscutting.annotations;
//
//import org.javers.core.diff.changetype.ValueChange;
//import org.javers.core.diff.changetype.container.ContainerChange;
//import org.javers.core.diff.changetype.map.MapChange;
//
//import javax.annotation.meta.TypeQualifier;
//import javax.annotation.meta.TypeQualifierValidator;
//import javax.annotation.meta.When;
//import java.lang.annotation.Documented;
//import java.lang.annotation.Retention;
//import java.lang.annotation.RetentionPolicy;
//
///**
// * The annotated element must be of type Change
// */
//@Documented
//@TypeQualifier
//@Retention(RetentionPolicy.RUNTIME)
//public @interface Nonnull {
//    When when() default When.ALWAYS;
//
//    class Checker implements TypeQualifierValidator<javax.annotation.Nonnull> {
//
//        public When forConstantValue(javax.annotation.Nonnull qualifierArgument, Object value) {
//            if (change instanceof ContainerChange) {
//                return handleContainerChange(indentation, (ContainerChange) change);
//            } else if (change instanceof ValueChange) {
//                return handleValueChange(indentation, (ValueChange) change);
//            } else if (change instanceof MapChange) {
//                return handleMapChange(indentation, (MapChange) change) ;
//            }
//            if (value == null)
//                return When.NEVER;
//            return When.ALWAYS;
//        }
//    }
//}