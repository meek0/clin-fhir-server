package bio.ferlab.clin.interceptors;

import bio.ferlab.clin.auth.RPTPermissionExtractor;
import bio.ferlab.clin.auth.data.Permission;
import bio.ferlab.clin.auth.data.UserPermissions;
import bio.ferlab.clin.auth.data.custom.Export;
import bio.ferlab.clin.auth.data.custom.Metadata;
import bio.ferlab.clin.interceptors.metatag.MetaTagAuthRuleTester;
import bio.ferlab.clin.utils.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRuleBuilder;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BioAuthInterceptor extends AuthorizationInterceptor {
    private final RPTPermissionExtractor permissionExtractor;
    private final MetaTagAuthRuleTester metaTagAuthRuleTester;

    public BioAuthInterceptor(RPTPermissionExtractor permissionExtractor, MetaTagAuthRuleTester metaTagAuthRuleTester) {
        this.permissionExtractor = permissionExtractor;
        this.metaTagAuthRuleTester = metaTagAuthRuleTester;
    }

    private <T extends Resource> void
    allowReadPermissionByType(IAuthRuleBuilder builder, Class<T> resourceType) {
        builder.allow().read().resourcesOfType(resourceType).withAnyId().withTester(metaTagAuthRuleTester).andThen();
    }

    private <T extends Resource> void
    allowCreatePermissionByType(IAuthRuleBuilder builder, Class<T> resourceType) {
        // don't use the default allow() because internally it 
        // can only contain rules for create() or write() but not both
        builder.allow("create").create().resourcesOfType(resourceType).withAnyId().withTester(metaTagAuthRuleTester).andThen();
        // this one will contain all our rules with 'create' scope only
    }

    private <T extends Resource> void
    allowWritePermissionByType(IAuthRuleBuilder builder, Class<T> resourceType) {
        // don't use the default allow() because internally it
        // can only contain rules for create() or write() but not both
        builder.allow("write").write().resourcesOfType(resourceType).withAnyId().withTester(metaTagAuthRuleTester).andThen();
        // this one will contain all our rules with 'update' scope only
    }

    private <T extends Resource> void
    allowDeletePermissionByType(IAuthRuleBuilder builder, Class<T> resourceType) {
        builder.allow().delete().resourcesOfType(resourceType).withAnyId().withTester(metaTagAuthRuleTester).andThen();
    }

    private void handlePermission(IAuthRuleBuilder builder, Permission<? extends Resource> permission) {
        if (permission.isRead()) {
            allowReadPermissionByType(builder, permission.getResourceType());
        }
        if (permission.isUpdate()) {
            allowWritePermissionByType(builder, permission.getResourceType());
        }
        if (permission.isCreate()) {
            allowCreatePermissionByType(builder, permission.getResourceType());
        }
        if (permission.isDelete()) {
            allowDeletePermissionByType(builder, permission.getResourceType());
        }
    }

    private IAuthRuleBuilder handleUserPermissions(UserPermissions userPermissions) {
        final var builder = new RuleBuilder();
        for (Permission<? extends Resource> permission : userPermissions.getPermissions()) {
            if (permission.resourceType.equals(Export.class)) {
                applyRulesOnExport(builder);
            } else if (permission.resourceType.equals(Metadata.class)) {
                applyRulesOnMetadata(builder);
            } else {
                handlePermission(builder, permission);
            }
        }
        return builder;
    }

    private void applyRulesOnTransactions(IAuthRuleBuilder ruleBuilder) {
        ruleBuilder.allow().transaction().withAnyOperation().andApplyNormalRules().andThen();
    }

    private void applyRulesOnGraphql(IAuthRuleBuilder ruleBuilder) {
        ruleBuilder.allow().graphQL().any().andThen();
    }

    private void applyRulesOnValidate(IAuthRuleBuilder ruleBuilder) {
        ruleBuilder.allow().operation().named(Constants.VALIDATE_OPERATION).onAnyType().andRequireExplicitResponseAuthorization().andThen();
    }

    private void applyRulesOnExport(IAuthRuleBuilder ruleBuilder) {
        // first allow export and status operations during SERVER_INCOMING_REQUEST_PRE_HANDLED
        ruleBuilder.allow().operation().named(Constants.EXPORT_OPERATION).onServer().andRequireExplicitResponseAuthorization().andThen();
        ruleBuilder.allow().operation().named(Constants.EXPORT_STATUS_OPERATION).onServer().andRequireExplicitResponseAuthorization().andThen();
        // then allow bulk export for all types during STORAGE_INITIATE_BULK_EXPORT
        ruleBuilder.allow().bulkExport().any().andThen();
    }

    private void applyRulesOnMetadata(IAuthRuleBuilder ruleBuilder) {
        ruleBuilder.allow().metadata().andThen();
    }

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails requestDetails) {
        final var permissions = this.permissionExtractor.extract(requestDetails);
        final var ruleBuilder = this.handleUserPermissions(permissions);
        this.applyRulesOnTransactions(ruleBuilder);
        this.applyRulesOnGraphql(ruleBuilder);
        this.applyRulesOnValidate(ruleBuilder);
        return ruleBuilder.denyAll().build();
    }
}
