package spring.defect.demo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.annotation.Bean
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import org.springframework.security.acls.AclPermissionEvaluator
import org.springframework.security.acls.domain.AclAuthorizationStrategy
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl
import org.springframework.security.acls.domain.ConsoleAuditLogger
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy
import org.springframework.security.acls.domain.SpringCacheBasedAclCache
import org.springframework.security.acls.jdbc.BasicLookupStrategy
import org.springframework.security.acls.jdbc.JdbcMutableAclService
import org.springframework.security.acls.jdbc.LookupStrategy
import org.springframework.security.acls.model.AclCache
import org.springframework.security.acls.model.AclService
import org.springframework.security.acls.model.MutableAclService
import org.springframework.security.acls.model.PermissionGrantingStrategy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import javax.sql.DataSource

@SpringBootApplication
@EnableReactiveMethodSecurity
class SecurityDemoApplication {
    @Bean
    fun aclAuthorizationStrategy(): AclAuthorizationStrategy {

        //Provide GrantedAuthority role that can be used to change Sids
        return AclAuthorizationStrategyImpl(SimpleGrantedAuthority("ROLE_ADMIN"))
    }

    @Bean
    fun permissionGrantingStrategy(): PermissionGrantingStrategy {
        return DefaultPermissionGrantingStrategy(ConsoleAuditLogger())
    }

    @Bean
    fun aclCache(): AclCache {
        val aclCache = ConcurrentMapCache("acl_cache")
        return SpringCacheBasedAclCache(aclCache, permissionGrantingStrategy(), aclAuthorizationStrategy())
    }


    @Bean
    fun aclService(
        dataSource: DataSource,
        lookupStrategy: LookupStrategy,
        aclCache: AclCache
    ): MutableAclService {
        return JdbcMutableAclService(dataSource, lookupStrategy, aclCache)
    }

    @Bean
    fun lookupStrategy(
        dataSource: DataSource,
        aclAuthorizationStrategy: AclAuthorizationStrategy,
        aclCache: AclCache
    ): LookupStrategy {
        return BasicLookupStrategy(
            dataSource,
            aclCache,
            aclAuthorizationStrategy,
            ConsoleAuditLogger()
        )
    }


    @Bean
    fun aclPermissionEvaluator(aclService: AclService): AclPermissionEvaluator {
        return AclPermissionEvaluator(aclService)
    }
}

@Service
class TestService {

    @PostAuthorize("hasPermission(returnObject, 'read')")
    fun getDocument(id: Int): Mono<Document> {
        return Mono.just(Document(id))
    }
}

data class Document(
    var id: Int? = null
)

fun main(args: Array<String>) {
    runApplication<SecurityDemoApplication>(*args)
}
