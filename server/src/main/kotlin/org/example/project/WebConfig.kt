package org.example.project

import org.example.project.domain.shared.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import kotlin.uuid.Uuid

@Configuration
class WebConfig : WebMvcConfigurer {
    @Bean
    fun kotlinSerializationJsonHttpMessageConverter(): KotlinSerializationJsonHttpMessageConverter {
        return KotlinSerializationJsonHttpMessageConverter()
    }

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(String::class.java, Uuid::class.java) { Uuid.parse(it) }
        registry.addConverter(String::class.java, OrderId::class.java) { OrderId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, SubOrderId::class.java) { SubOrderId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, CharacterId::class.java) { CharacterId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, ProductId::class.java) { ProductId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, MerchantId::class.java) { MerchantId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, ShippingMethodId::class.java) { ShippingMethodId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, TransactionId::class.java) { TransactionId(Uuid.parse(it)) }
        registry.addConverter(String::class.java, CurrencyId::class.java) { CurrencyId(Uuid.parse(it)) }
    }
}
