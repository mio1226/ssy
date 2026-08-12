package com.harddisk.module.rule.controller;

import com.harddisk.common.Result;
import com.harddisk.common.PageResult;
import com.harddisk.module.rule.entity.RuleConfig;
import com.harddisk.module.rule.entity.ViolationRecord;
import com.harddisk.module.rule.service.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rule")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @GetMapping("/violations")
    public Result<PageResult<ViolationRecord>> listViolations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        var p = ruleService.listViolations(page, pageSize, type, status);
        return Result.success(PageResult.of(p.getTotal(), p.getCurrent(), p.getSize(), p.getRecords()));
    }

    @PutMapping("/violations/{id}/resolve")
    public Result<Void> resolveViolation(@PathVariable Long id) {
        ruleService.resolveViolation(id);
        return Result.success();
    }

    @GetMapping("/configs")
    public Result<List<RuleConfig>> getRuleConfigs() {
        return Result.success(ruleService.getRuleConfigs());
    }

    @PutMapping("/configs")
    public Result<RuleConfig> updateRuleConfig(@RequestBody RuleConfig config) {
        return Result.success(ruleService.updateRuleConfig(config));
    }
}
