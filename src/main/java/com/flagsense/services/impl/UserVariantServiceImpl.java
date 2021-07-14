package com.flagsense.services.impl;

import com.flagsense.enums.Operator;
import com.flagsense.enums.Status;
import com.flagsense.model.*;
import com.flagsense.services.UserVariantService;
import com.flagsense.util.FlagsenseException;
import com.flagsense.util.MurmurHash3;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.util.Version;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.flagsense.util.Constants.MAX_HASH_VALUE;
import static com.flagsense.util.Constants.TOTAL_THREE_DECIMAL_TRAFFIC;

public class UserVariantServiceImpl implements UserVariantService {

    private final Data data;

    public UserVariantServiceImpl(Data data) {
        this.data = data;
    }

    @Override
    public void getUserVariant(UserVariantDTO userVariantDTO) {
        if (StringUtils.isBlank(userVariantDTO.getUserId()))
            throw new FlagsenseException("Bad user");

        FlagDTO flagDTO = getFlagData(userVariantDTO.getFlagId());
        if (flagDTO == null)
            throw new FlagsenseException("Flag not found");

        if (flagDTO.getType() != userVariantDTO.getExpectedVariantType())
            throw new FlagsenseException("Bad flag type specified");

        String userVariantKey = getUserVariantKey(userVariantDTO, flagDTO);
        userVariantDTO.setKey(userVariantKey);
        userVariantDTO.setValue(flagDTO.getVariants().get(userVariantKey).getValue());
    }

    private FlagDTO getFlagData(String flagId) {
        if (this.data == null || this.data.getFlags() == null)
            return null;
        return this.data.getFlags().get(flagId);
    }

    private Map<String, SegmentDTO> getSegmentsMap(EnvData envData) {
        if (this.data == null || this.data.getSegments() == null)
            return new HashMap<>();
        return this.data.getSegments();
    }

    private String getUserVariantKey(UserVariantDTO userVariantDTO, FlagDTO flagDTO) {
        String userId = userVariantDTO.getUserId();
        Map<String, Object> attributes = userVariantDTO.getAttributes();

        EnvData envData = flagDTO.getEnvData();
        if (envData.getStatus() == Status.INACTIVE)
            return envData.getOffVariant();

        Map<String, SegmentDTO> segmentsMap = getSegmentsMap(envData);
        if (!matchesPrerequisites(userId, attributes, envData.getPrerequisites(), segmentsMap))
            return envData.getOffVariant();

        Map<String, String> targetUsers = envData.getTargetUsers();
        if (targetUsers != null && targetUsers.containsKey(userId))
            return targetUsers.get(userId);

        List<String> targetSegmentsOrder = envData.getTargetSegmentsOrder();
        if (targetSegmentsOrder != null) {
            for (String targetSegment : targetSegmentsOrder) {
                if (isUserInSegment(userId, attributes, segmentsMap.get(targetSegment)))
                    return allocateTrafficVariant(userId, flagDTO, envData.getTargetSegments().get(targetSegment));
            }
        }

        return allocateTrafficVariant(userId, flagDTO, envData.getTraffic());
    }

    private boolean matchesPrerequisites(String userId, Map<String, Object> attributes, List<String> prerequisites, Map<String, SegmentDTO> segmentsMap) {
        if (prerequisites == null)
            return true;

        for (String prerequisite : prerequisites) {
            if (!isUserInSegment(userId, attributes, segmentsMap.get(prerequisite)))
                return false;
        }

        return true;
    }

    private boolean isUserInSegment(String userId, Map<String, Object> attributes, SegmentDTO segmentDTO) {
        if (segmentDTO == null)
            return false;

        List<List<Rule>> andRules = segmentDTO.getRules();
        for (List<Rule> andRule : andRules) {
            if (!matchesAndRule(userId, attributes, andRule))
                return false;
        }

        return true;
    }

    private boolean matchesAndRule(String userId, Map<String, Object> attributes, List<Rule> orRules) {
        for (Rule orRule : orRules) {
            if (matchesRule(userId, attributes, orRule))
                return true;
        }
        return false;
    }

    private boolean matchesRule(String userId, Map<String, Object> attributes, Rule rule) {
        Object attributeValue = getAttributeValue(userId, attributes, rule.getKey());
        if (attributeValue == null)
            return false;

        try {
            boolean userMatchesRule;

            switch (rule.getType()) {
                case INT:
                    userMatchesRule = matchesIntRule(rule, (Integer) attributeValue);
                    break;

                case BOOL:
                    userMatchesRule = matchesBoolRule(rule, (Boolean) attributeValue);
                    break;

                case DOUBLE:
                    userMatchesRule = matchesDoubleRule(rule, Double.parseDouble(attributeValue.toString()));
                    break;

                case STRING:
                    userMatchesRule = matchesStringRule(rule, (String) attributeValue);
                    break;

                case VERSION:
                    userMatchesRule = matchesVersionRule(rule, (String) attributeValue);
                    break;

                default:
                    userMatchesRule = false;
            }

            return userMatchesRule == rule.getMatch();
        }
        catch (ClassCastException e) {
            // log.info("ClassCastException for type " + rule.getType() + ", key:" + rule.getKey() + ", value:" + attributeValue);
            return false;
        }
        catch (Exception e) {
            // log.error("Exception in matchesRule", e);
            return false;
        }
    }

    private Object getAttributeValue(String userId, Map<String, Object> attributes, String key) {
        if (StringUtils.equals(key, "id"))
            return userId;
        return attributes == null ? null : attributes.get(key);
    }

    private boolean matchesIntRule(Rule<Integer> rule, Integer attributeValue) {
        List<Integer> values = rule.getValues();
        switch (rule.getOperator()) {
            case LT:
                return attributeValue.compareTo(values.get(0)) < 0;

            case LTE:
                return attributeValue.compareTo(values.get(0)) <= 0;

            case EQ:
                return attributeValue.compareTo(values.get(0)) == 0;

            case GT:
                return attributeValue.compareTo(values.get(0)) > 0;

            case GTE:
                return attributeValue.compareTo(values.get(0)) >= 0;

            case IOF:
                return values.contains(attributeValue);

            default:
                return false;
        }
    }

    private boolean matchesBoolRule(Rule<Boolean> rule, Boolean attributeValue) {
        List<Boolean> values = rule.getValues();

        if (rule.getOperator() == Operator.EQ)
            return attributeValue.compareTo(values.get(0)) == 0;

        return false;
    }

    private boolean matchesDoubleRule(Rule<Double> rule, Double attributeValue) {
        List<Double> values = rule.getValues();
        switch (rule.getOperator()) {
            case LT:
                return attributeValue.compareTo(values.get(0)) < 0;

            case LTE:
                return attributeValue.compareTo(values.get(0)) <= 0;

            case EQ:
                return attributeValue.compareTo(values.get(0)) == 0;

            case GT:
                return attributeValue.compareTo(values.get(0)) > 0;

            case GTE:
                return attributeValue.compareTo(values.get(0)) >= 0;

            case IOF:
                return values.contains(attributeValue);

            default:
                return false;
        }
    }

    private boolean matchesStringRule(Rule<String> rule, String attributeValue) {
        List<String> values = rule.getValues();
        switch (rule.getOperator()) {
            case EQ:
                return StringUtils.equals(attributeValue, values.get(0));

            case HAS:
                return StringUtils.contains(attributeValue, values.get(0));

            case SW:
                return StringUtils.startsWith(attributeValue, values.get(0));

            case EW:
                return StringUtils.endsWith(attributeValue, values.get(0));

            case IOF:
                return values.contains(attributeValue);

            default:
                return false;
        }
    }

    private boolean matchesVersionRule(Rule<String> rule, String attributeValue) {
        List<String> values = rule.getValues();
        Version version = Version.parse(attributeValue);

        switch (rule.getOperator()) {
            case LT:
                return version.isLessThan(Version.parse(values.get(0)));

            case LTE:
                return version.isLessThanOrEqualTo(Version.parse(values.get(0)));

            case EQ:
                return version.equals(Version.parse(values.get(0)));

            case GT:
                return version.isGreaterThan(Version.parse(values.get(0)));

            case GTE:
                return version.isGreaterThanOrEqualTo(Version.parse(values.get(0)));

            case IOF:
                return values.contains(version.toString());

            default:
                return false;
        }
    }

    private String allocateTrafficVariant(String userId, FlagDTO flagDTO, Map<String, Integer> traffic) {
        if (traffic.size() == 1)
            return traffic.keySet().iterator().next();

        String bucketingId = userId + flagDTO.getId();
        List<String> variantsOrder = flagDTO.getVariantsOrder();

        int hashCode = MurmurHash3.murmurhash3_x86_32(bucketingId, 0, bucketingId.length(), flagDTO.getSeed());
        double ratio = (double) (hashCode & 0xFFFFFFFFL) / MAX_HASH_VALUE;
        int bucketValue = (int) Math.floor(TOTAL_THREE_DECIMAL_TRAFFIC * ratio);

        int endOfRange = 0;
        for (String variant : variantsOrder) {
            endOfRange += traffic.getOrDefault(variant, 0);
            if (bucketValue < endOfRange)
                return variant;
        }

        return variantsOrder.get(variantsOrder.size() - 1);
    }
}
