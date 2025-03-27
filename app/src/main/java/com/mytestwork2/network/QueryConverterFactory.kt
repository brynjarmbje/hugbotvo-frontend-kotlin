package com.mytestwork2.network

import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

// Add this to your network package
class QueryConverterFactory : Converter.Factory() {
    override fun stringConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, String>? {
        if (type is ParameterizedType) {
            val rawType = type.rawType
            if (rawType == List::class.java) {
                return Converter<List<*>, String> { list ->
                    list.joinToString(",")
                }
            }
        }
        return null
    }
}
