package gascolae.group9.package_builder.customer.mapper;

import gascolae.group9.package_builder.customer.dto.request.ExpectedOutputRequest;
import gascolae.group9.package_builder.customer.dto.request.RequirementCreateRequest;
import gascolae.group9.package_builder.customer.dto.request.RequirementUpdateRequest;
import gascolae.group9.package_builder.customer.dto.response.ExpectedOutputResponse;
import gascolae.group9.package_builder.customer.dto.response.RequirementResponse;
import gascolae.group9.package_builder.customer.entity.CustomerRequirement;
import gascolae.group9.package_builder.customer.entity.RequirementExpectedOutput;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RequirementMapper {

    @Mapping(target = "requirementId", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "confirmedBy", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "expectedOutputs", ignore = true)
    @Mapping(target = "extractionMethod", ignore = true)
    @Mapping(target = "extractionConfidence", ignore = true)
    @Mapping(target = "areaValue", expression = "java(request.getAreaValueAsBigDecimal())")
    @Mapping(target = "level", expression = "java(request.getLevelAsInteger())")
    CustomerRequirement toRequirement(RequirementCreateRequest request);

    @Mapping(target = "requirementId", ignore = true)
    @Mapping(target = "requirementCode", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "confirmedBy", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "expectedOutputs", ignore = true)
    @Mapping(target = "extractionMethod", ignore = true)
    @Mapping(target = "extractionConfidence", ignore = true)
    @Mapping(target = "areaValue", expression = "java(request.getAreaValueAsBigDecimal())")
    @Mapping(target = "areaUnit", expression = "java(request.getAreaUnit())")
    @Mapping(target = "level", expression = "java(request.getLevelAsInteger())")
    CustomerRequirement toRequirementFromLead(gascolae.group9.package_builder.customer.dto.request.LandingLeadRequest request);

    @Mapping(target = "customerId", source = "customer.customerId")
    @Mapping(target = "customerCode", source = "customer.customerCode")
    @Mapping(target = "customerName", source = "customer.customerName")
    @Mapping(target = "companyName", source = "customer.companyName")
    @Mapping(target = "contactEmail", source = "customer.contactEmail")
    @Mapping(target = "contactPhone", source = "customer.contactPhone")
    RequirementResponse toRequirementResponse(CustomerRequirement requirement);

    List<RequirementResponse> toRequirementResponseList(List<CustomerRequirement> requirements);

    @Mapping(target = "requirementExpectedOutputId", ignore = true)
    @Mapping(target = "requirement", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    RequirementExpectedOutput toExpectedOutput(ExpectedOutputRequest request);

    ExpectedOutputResponse toExpectedOutputResponse(RequirementExpectedOutput output);

    List<ExpectedOutputResponse> toExpectedOutputResponseList(List<RequirementExpectedOutput> outputs);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "requirementId", ignore = true)
    @Mapping(target = "requirementCode", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "confirmedBy", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "expectedOutputs", ignore = true)
    @Mapping(target = "extractionMethod", ignore = true)
    @Mapping(target = "extractionConfidence", ignore = true)
    @Mapping(target = "areaValue", expression = "java(request.getAreaValueAsBigDecimal())")
    @Mapping(target = "level", expression = "java(request.getLevelAsInteger())")
    @Mapping(target = "areaValue", expression = "java(request.getAreaValue() != null ? request.getAreaValueAsBigDecimal() : requirement.getAreaValue())")
    @Mapping(target = "level", expression = "java(request.getLevel() != null ? request.getLevelAsInteger() : requirement.getLevel())")
    void updateRequirementFromRequest(RequirementUpdateRequest request, @MappingTarget CustomerRequirement requirement);
}
