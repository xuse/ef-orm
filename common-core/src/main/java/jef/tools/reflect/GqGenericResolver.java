package jef.tools.reflect;

import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jef.tools.Assert;

final class GqGenericResolver {
	private static final Logger log=LoggerFactory.getLogger(GqGenericResolver.class);
	static final Type[] EMPTY_TYPE_ARRAY = new Type[] {};

	/**
	 * Returns a new parameterized type, applying {@code typeArguments} to
	 * {@code rawType} and enclosed by {@code ownerType}.
	 * 
	 * @return a {@link java.io.Serializable serializable} parameterized type.
	 */
	public static ParameterizedType newParameterizedTypeWithOwner(Type ownerType, Type rawType, Type... typeArguments) {
		return new ParameterizedTypeImpl(ownerType, rawType, typeArguments);
	}

	/**
	 * Returns an array type whose elements are all instances of
	 * {@code componentType}.
	 * 
	 * @return a {@link java.io.Serializable serializable} generic array type.
	 */
	public static GenericArrayType arrayOf(Type componentType) {
		return new GenericArrayTypeImpl(componentType);
	}

	/**
	 * Returns a type that represents an unknown type that extends {@code bound}
	 * . For example, if {@code bound} is {@code CharSequence.class}, this
	 * returns {@code ? extends CharSequence}. If {@code bound} is
	 * {@code Object.class}, this returns {@code ?}, which is shorthand for
	 * {@code ?
	 * extends Object}.
	 */
	public static WildcardType subtypeOf(Type bound) {
		return new WildcardTypeImpl(new Type[] { bound }, EMPTY_TYPE_ARRAY);
	}

	/**
	 * Returns a type that represents an unknown supertype of {@code bound}. For
	 * example, if {@code bound} is {@code String.class}, this returns {@code ?
	 * super String}.
	 */
	public static WildcardType supertypeOf(Type bound) {
		return new WildcardTypeImpl(new Type[] { Object.class }, new Type[] { bound });
	}

	/**
	 * Returns a type that is functionally equal but not necessarily equal
	 * according to {@link Object#equals(Object) Object.equals()}. The returned
	 * type is {@link java.io.Serializable}.
	 */
	public static Type canonicalize(Type type) {
		if (type instanceof Class) {
			Class<?> c = (Class<?>) type;
			return c.isArray() ? new GenericArrayTypeImpl(canonicalize(c.getComponentType())) : c;

		} else if (type instanceof ParameterizedType) {
			ParameterizedType p = (ParameterizedType) type;
			return new ParameterizedTypeImpl(p.getOwnerType(), p.getRawType(), p.getActualTypeArguments());

		} else if (type instanceof GenericArrayType) {
			GenericArrayType g = (GenericArrayType) type;
			return new GenericArrayTypeImpl(g.getGenericComponentType());

		} else if (type instanceof WildcardType) {
			WildcardType w = (WildcardType) type;
			return new WildcardTypeImpl(w.getUpperBounds(), w.getLowerBounds());

		} else {
			// type is either serializable as-is or unsupported
			return type;
		}
	}

	static boolean equal(Object a, Object b) {
		return a == b || (a != null && a.equals(b));
	}

	/**
	 * Returns true if {@code a} and {@code b} are equal.
	 */
	public static boolean equals(Type a, Type b) {
		if (a == b) {
			// also handles (a == null && b == null)
			return true;

		} else if (a instanceof Class) {
			// Class already specifies equals().
			return a.equals(b);

		} else if (a instanceof ParameterizedType) {
			if (!(b instanceof ParameterizedType)) {
				return false;
			}

			ParameterizedType pa = (ParameterizedType) a;
			ParameterizedType pb = (ParameterizedType) b;
			return equal(pa.getOwnerType(), pb.getOwnerType()) && pa.getRawType().equals(pb.getRawType()) && Arrays.equals(pa.getActualTypeArguments(), pb.getActualTypeArguments());

		} else if (a instanceof GenericArrayType) {
			if (!(b instanceof GenericArrayType)) {
				return false;
			}

			GenericArrayType ga = (GenericArrayType) a;
			GenericArrayType gb = (GenericArrayType) b;
			return equals(ga.getGenericComponentType(), gb.getGenericComponentType());

		} else if (a instanceof WildcardType) {
			if (!(b instanceof WildcardType)) {
				return false;
			}

			WildcardType wa = (WildcardType) a;
			WildcardType wb = (WildcardType) b;
			return Arrays.equals(wa.getUpperBounds(), wb.getUpperBounds()) && Arrays.equals(wa.getLowerBounds(), wb.getLowerBounds());

		} else if (a instanceof TypeVariable) {
			if (!(b instanceof TypeVariable)) {
				return false;
			}
			TypeVariable<?> va = (TypeVariable<?>) a;
			TypeVariable<?> vb = (TypeVariable<?>) b;
			return va.getGenericDeclaration() == vb.getGenericDeclaration() && va.getName().equals(vb.getName());

		} else {
			// This isn't a type we support. Could be a generic array type,
			// wildcard type, etc.
			return false;
		}
	}

	private static int hashCodeOrZero(Object o) {
		return o != null ? o.hashCode() : 0;
	}

	public static String typeToString(Type type) {
		return type instanceof Class ? ((Class<?>) type).getName() : type.toString();
	}

	/**
	 * Returns the generic supertype for {@code supertype}. For example, given a
	 * class {@code IntegerSet}, the result for when supertype is
	 * {@code Set.class} is {@code Set<Integer>} and the result when the
	 * supertype is {@code Collection.class} is {@code Collection<Integer>}.
	 */
	static Type getGenericSupertype(Type context, Class<?> rawType, Class<?> toResolve) {
		if (toResolve == rawType) {
			return context;
		}

		// we skip searching through interfaces if unknown is an interface
		if (toResolve.isInterface()) {
			Class<?>[] interfaces = rawType.getInterfaces();
			for (int i = 0, length = interfaces.length; i < length; i++) {
				if (interfaces[i] == toResolve) {
					return rawType.getGenericInterfaces()[i];
				} else if (toResolve.isAssignableFrom(interfaces[i])) {
					return getGenericSupertype(rawType.getGenericInterfaces()[i], interfaces[i], toResolve);
				}
			}
		}

		// check our supertypes
		if (!rawType.isInterface()) {
			while (rawType != Object.class) {
				Class<?> rawSupertype = rawType.getSuperclass();
				if (rawSupertype == toResolve) {
					return rawType.getGenericSuperclass();
				} else if (toResolve.isAssignableFrom(rawSupertype)) {
					return getGenericSupertype(rawType.getGenericSuperclass(), rawSupertype, toResolve);
				}
				rawType = rawSupertype;
			}
		}

		// we can't resolve this further
		return toResolve;
	}

	/**
	 * Returns true if this type is an array.
	 */
	public static boolean isArray(Type type) {
		return type instanceof GenericArrayType || (type instanceof Class && ((Class<?>) type).isArray());
	}

	/**
	 * Returns the component type of this array type.
	 * 
	 * @throws ClassCastException
	 *             if this type is not an array.
	 */
	public static Type getArrayComponentType(Type array) {
		return array instanceof GenericArrayType ? ((GenericArrayType) array).getGenericComponentType() : ((Class<?>) array).getComponentType();
	}

	public static Type resolve(Type context, Class<?> contextRawType, Type toResolve) {
		// this implementation is made a little more complicated in an attempt
		// to avoid object-creation
		while (true) {
			if (toResolve instanceof TypeVariable) {
				TypeVariable<?> typeVariable = (TypeVariable<?>) toResolve;
				toResolve = resolveTypeVariable(context, contextRawType, typeVariable);
				if (toResolve == typeVariable) {
					return toResolve;
				}

			} else if (toResolve instanceof Class && ((Class<?>) toResolve).isArray()) {
				Class<?> original = (Class<?>) toResolve;
				Type componentType = original.getComponentType();
				Type newComponentType = resolve(context, contextRawType, componentType);
				return componentType == newComponentType ? original : arrayOf(newComponentType);

			} else if (toResolve instanceof GenericArrayType) {
				GenericArrayType original = (GenericArrayType) toResolve;
				Type componentType = original.getGenericComponentType();
				Type newComponentType = resolve(context, contextRawType, componentType);
				return componentType == newComponentType ? original : arrayOf(newComponentType);

			} else if (toResolve instanceof ParameterizedType) {
				ParameterizedType original = (ParameterizedType) toResolve;
				Type ownerType = original.getOwnerType();
				Type newOwnerType = resolve(context, contextRawType, ownerType);
				boolean changed = newOwnerType != ownerType;

				Type[] args = original.getActualTypeArguments();
				for (int t = 0, length = args.length; t < length; t++) {
					Type resolvedTypeArgument = resolve(context, contextRawType, args[t]);
					if (resolvedTypeArgument != args[t]) {
						if (!changed) {
							args = args.clone();
							changed = true;
						}
						args[t] = resolvedTypeArgument;
					}
				}

				return changed ? newParameterizedTypeWithOwner(newOwnerType, original.getRawType(), args) : original;

			} else if (toResolve instanceof WildcardType) {
				WildcardType original = (WildcardType) toResolve;
				Type[] originalLowerBound = original.getLowerBounds();
				Type[] originalUpperBound = original.getUpperBounds();

				if (originalLowerBound.length == 1) {
					Type lowerBound = resolve(context, contextRawType, originalLowerBound[0]);
					if (lowerBound != originalLowerBound[0]) {
						return supertypeOf(lowerBound);
					}
				} else if (originalUpperBound.length == 1) {
					Type upperBound = resolve(context, contextRawType, originalUpperBound[0]);
					if (upperBound != originalUpperBound[0]) {
						return subtypeOf(upperBound);
					}
				}
				return original;

			} else {
				return toResolve;
			}
		}
	}

	static Type resolveTypeVariable(Type context, Class<?> contextRawType, TypeVariable<?> unknown) {
		Class<?> declaredByRaw = declaringClassOf(unknown);

		// we can't reduce this further
		if (declaredByRaw == null) {
			return unknown;
		}

		Type declaredBy = getGenericSupertype(context, contextRawType, declaredByRaw);
		if (declaredBy instanceof ParameterizedType) {
			int index = indexOf(declaredByRaw.getTypeParameters(), unknown);
			return ((ParameterizedType) declaredBy).getActualTypeArguments()[index];
		}

		return unknown;
	}

	private static int indexOf(Object[] array, Object toFind) {
		for (int i = 0; i < array.length; i++) {
			if (toFind.equals(array[i])) {
				return i;
			}
		}
		throw new NoSuchElementException("Not found:" + toFind);
	}

	/**
	 * Returns the declaring class of {@code typeVariable}, or {@code null} if
	 * it was not declared by a class.
	 */
	private static Class<?> declaringClassOf(TypeVariable<?> typeVariable) {
		GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
		return genericDeclaration instanceof Class ? (Class<?>) genericDeclaration : null;
	}

	private static void checkNotPrimitive(Type type) {
		Assert.isTrue(!(type instanceof Class<?>) || !((Class<?>) type).isPrimitive());
	}

	private static final class ParameterizedTypeImpl implements ParameterizedType, Serializable {
		private final Type ownerType;
		private final Type rawType;
		private final Type[] typeArguments;

		public ParameterizedTypeImpl(Type ownerType, Type rawType, Type... typeArguments) {
			// require an owner type if the raw type needs it
			if (rawType instanceof Class<?>) {
				Class<?> rawTypeAsClass = (Class<?>) rawType;
				try {
					Assert.isTrue(ownerType != null || rawTypeAsClass.getEnclosingClass() == null);
					Assert.isTrue(ownerType == null || rawTypeAsClass.getEnclosingClass() != null);
				} catch (RuntimeException e) {
					// R][2011-09-27 21:08:14,570] [ORM._log] ownerType:null
					// [ERROR][2011-09-27 21:08:14,572] [ORM._log]
					// rawTypeAsClass:interface java.util.Map$Entry
					// [ERROR][2011-09-27 21:08:14,573] [ORM._log]
					// getEnclosingClass:interface java.util.Map

					log.error("ownerType:" + ownerType);
					log.error("rawTypeAsClass:" + rawTypeAsClass);
					log.error("getEnclosingClass:" + rawTypeAsClass.getEnclosingClass());
					throw e;
				}

			}

			this.ownerType = ownerType == null ? null : canonicalize(ownerType);
			this.rawType = canonicalize(rawType);
			this.typeArguments = typeArguments.clone();
			for (int t = 0; t < this.typeArguments.length; t++) {
				Assert.notNull(this.typeArguments[t]);
				checkNotPrimitive(this.typeArguments[t]);
				this.typeArguments[t] = canonicalize(this.typeArguments[t]);
			}
		}

		public Type[] getActualTypeArguments() {
			return typeArguments.clone();
		}

		public Type getRawType() {
			return rawType;
		}

		public Type getOwnerType() {
			return ownerType;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof ParameterizedType && GqGenericResolver.equals(this, (ParameterizedType) other);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(typeArguments) ^ rawType.hashCode() ^ hashCodeOrZero(ownerType);
		}

		@Override
		public String toString() {
			StringBuilder stringBuilder = new StringBuilder(30 * (typeArguments.length + 1));
			stringBuilder.append(typeToString(rawType));

			if (typeArguments.length == 0) {
				return stringBuilder.toString();
			}

			stringBuilder.append("<").append(typeToString(typeArguments[0]));
			for (int i = 1; i < typeArguments.length; i++) {
				stringBuilder.append(", ").append(typeToString(typeArguments[i]));
			}
			return stringBuilder.append(">").toString();
		}

		private static final long serialVersionUID = 0;
	}

	private static final class GenericArrayTypeImpl implements GenericArrayType, Serializable {
		private final Type componentType;

		public GenericArrayTypeImpl(Type componentType) {
			this.componentType = canonicalize(componentType);
		}

		public Type getGenericComponentType() {
			return componentType;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof GenericArrayType && GqGenericResolver.equals(this, (GenericArrayType) o);
		}

		@Override
		public int hashCode() {
			return componentType.hashCode();
		}

		@Override
		public String toString() {
			return typeToString(componentType) + "[]";
		}

		private static final long serialVersionUID = 0;
	}

	/**
	 * The WildcardType interface supports multiple upper bounds and multiple
	 * lower bounds. We only support what the Java 6 language needs - at most
	 * one bound. If a lower bound is set, the upper bound must be Object.class.
	 */
	private static final class WildcardTypeImpl implements WildcardType, Serializable {
		private final Type upperBound;
		private final Type lowerBound;

		public WildcardTypeImpl(Type[] upperBounds, Type[] lowerBounds) {
			Assert.isTrue(lowerBounds.length <= 1);
			Assert.isTrue(upperBounds.length == 1);

			if (lowerBounds.length == 1) {
				Assert.notNull(lowerBounds[0]);
				checkNotPrimitive(lowerBounds[0]);
				Assert.isTrue(upperBounds[0] == Object.class);
				this.lowerBound = canonicalize(lowerBounds[0]);
				this.upperBound = Object.class;

			} else {
				Assert.notNull(upperBounds[0]);
				checkNotPrimitive(upperBounds[0]);
				this.lowerBound = null;
				this.upperBound = canonicalize(upperBounds[0]);
			}
		}

		public Type[] getUpperBounds() {
			return new Type[] { upperBound };
		}

		public Type[] getLowerBounds() {
			return lowerBound != null ? new Type[] { lowerBound } : EMPTY_TYPE_ARRAY;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof WildcardType && GqGenericResolver.equals(this, (WildcardType) other);
		}

		@Override
		public int hashCode() {
			// this equals Arrays.hashCode(getLowerBounds()) ^
			// Arrays.hashCode(getUpperBounds());
			return (lowerBound != null ? 31 + lowerBound.hashCode() : 1) ^ (31 + upperBound.hashCode());
		}

		@Override
		public String toString() {
			if (lowerBound != null) {
				return "? super " + typeToString(lowerBound);
			} else if (upperBound == Object.class) {
				return "?";
			} else {
				return "? extends " + typeToString(upperBound);
			}
		}

		private static final long serialVersionUID = 0;
	}
}
