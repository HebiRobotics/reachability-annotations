package us.hebi.graalvm.reachability.annotations;

/**
 * Specifies the member access levels for reflective configurations
 *
 * @author Florian Enner
 * @since 13 Jul 2026
 */
public enum MemberAccess {
    ALL_DECLARED_CLASSES, // Register classes which would be returned by the java.lang.Class#getDeclaredClasses call
    ALL_DECLARED_METHODS, // Register methods which would be returned by the java.lang.Class#getDeclaredMethods call
    ALL_DECLARED_FIELDS, // Register fields which would be returned by the java.lang.Class#getDeclaredFields call
    ALL_DECLARED_CONSTRUCTORS, // Register constructors which would be returned by the java.lang.Class#getDeclaredConstructors call
    ALL_PUBLIC_CLASSES, // Register all public classes which would be returned by the java.lang.Class#getClasses call
    ALL_PUBLIC_METHODS, // Register all public methods which would be returned by the java.lang.Class#getMethods call
    ALL_PUBLIC_FIELDS, // Register all public fields which would be returned by the java.lang.Class#getFields call
    ALL_PUBLIC_CONSTRUCTORS, // Register all public constructors which would be returned by the java.lang.Class#getConstructors call
    ALL_RECORD_COMPONENTS, // Register record components which would be returned by the java.lang.Class#getRecordComponents call
    ALL_PERMITTED_SUBCLASSES, // Register permitted subclasses which would be returned by the java.lang.Class#getPermittedSubclasses call
    ALL_NEST_MEMBERS, // Register nest members which would be returned by the java.lang.Class#getNestMembers call
    ALL_SIGNERS, // Register signers which would be returned by the java.lang.Class#getSigners call
    QUERY_ALL_DECLARED_METHODS, // Register methods which would be returned by the java.lang.Class#getDeclaredMethods call but only for lookup
    QUERY_ALL_DECLARED_CONSTRUCTORS, // Register constructors which would be returned by the java.lang.Class#getDeclaredConstructors call but only for lookup
    QUERY_ALL_PUBLIC_METHODS, // Register all public methods which would be returned by the java.lang.Class#getMethods call but only for lookup
    QUERY_ALL_PUBLIC_CONSTRUCTORS, // Register all public constructors which would be returned by the java.lang.Class#getConstructors call but only for lookup
    UNSAFE_ALLOCATED; // Allow objects of this class to be instantiated with a call to jdk.internal.misc.Unsafe#allocateInstance
}
