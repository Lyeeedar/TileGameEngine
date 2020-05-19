package com.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.lyeeedar.Util.FastEnumMap;

public class FastEnumMapSerializer extends Serializer<FastEnumMap<? extends Enum<?>, ?>>
{
	@Override
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public FastEnumMap<? extends Enum<?>, ?> copy( final Kryo kryo, final FastEnumMap<? extends Enum<?>, ?> original )
	{
		final FastEnumMap copy = new FastEnumMap( original );
		kryo.reference( copy );

		for ( int i = 0; i < copy.numItems(); i++ )
		{
			copy.put( i, kryo.copy( original.get( i ) ) );
		}
		return copy;
	}

	@Override
	public void write( final Kryo kryo, final Output output, final FastEnumMap<? extends Enum<?>, ?> map )
	{
		kryo.writeClass( output, map.keyType );

		output.writeInt( map.getSize(), true );
		for ( int i = 0; i < map.numItems(); i++ )
		{
			Object val = map.get( i );

			if ( val != null )
			{
				output.writeInt( i, true );
				kryo.writeClassAndObject( output, val );
			}
		}
	}

	@Override
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public FastEnumMap<? extends Enum<?>, ?> read( Kryo kryo, Input input, Class<? extends FastEnumMap<? extends Enum<?>, ?>> type ) {

		final Class<? extends Enum<?>> keyType = kryo.readClass( input ).getType();
		final Enum<?>[] enumConstants = keyType.getEnumConstants();

		final FastEnumMap rawResult = new FastEnumMap( keyType );
		kryo.reference( rawResult );

		final int size = input.readInt( true );
		for ( int i = 0; i < size; i++ )
		{
			final int ordinal = input.readInt( true );
			final Enum<?> key = enumConstants[ordinal];

			final Object value = kryo.readClassAndObject( input );

			rawResult.put( key, value );
		}

		return rawResult;
	}
}
