package spring.defect.demo

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.model.MutableAcl
import org.springframework.security.acls.model.MutableAclService
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.stereotype.Service
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import reactor.test.StepVerifier

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SecurityDemoApplicationTests {

    @Autowired
    lateinit var aclSetupService: AclSetupService

    @Autowired
    lateinit var aclService: MutableAclService

    @Autowired
    lateinit var testService: TestService

    @BeforeEach
    fun setUp() {
        aclSetupService.setupAcl {
            val sid = ObjectIdentityImpl(Document::class.java, 1)
            val acl = aclService.createAcl(sid) as MutableAcl
            acl.insertAce(0, BasePermission.READ, GrantedAuthoritySid("ROLE_USERS"), true)
            aclService.updateAcl(acl)
        }

    }

    @Test
    @WithMockUser(username = "User1", roles = ["USERS"])
    fun `Permission should be granted to ROLE_USERS`() {
        StepVerifier
            .create(testService.getDocument(1))
            .expectNext(Document(1))
            .verifyComplete()

    }


}
@Service
class AclSetupService(private val aclService: MutableAclService) {

    @Transactional(propagation = Propagation.REQUIRED)
    fun setupAcl(function: (MutableAclService) -> MutableAcl): MutableAcl {
        return function(aclService)
    }
}