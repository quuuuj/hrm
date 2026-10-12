package com.qiujie.dept.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.common.dto.Response;
import com.qiujie.common.dto.ResponseDTO;
import com.qiujie.dept.entity.Dept;
import com.qiujie.dept.mapper.DeptMapper;
import com.qiujie.util.EasyExcelUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.math.BigDecimal;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author qiujie
 * @since 2022-03-07
 */
@Service
public class DeptService extends ServiceImpl<DeptMapper, Dept> {

    @Autowired
    private DeptMapper deptMapper;

    public ResponseDTO add(Dept dept) {
        // 父级部门为 0 或未指定时视为根部门，不需要计算上班时间
        if (dept.getParentId() == null) {
            dept.setParentId(0);
        }
        if (dept.getParentId() != 0) {
            dept.setTotalWorkTime(calculateTotalWorkTime(dept));
        }
        if (save(dept)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO delete(Integer id) {
        // 级联检查：是否存在子部门
        Long childCount = this.deptMapper.selectCount(new QueryWrapper<Dept>().eq("parent_id", id));
        if (childCount > 0) {
            return Response.error("该部门下存在子部门，无法删除");
        }
        // 级联检查：是否存在关联员工
        Long staffCount = this.deptMapper.countStaffByDeptId(id);
        if (staffCount > 0) {
            return Response.error("该部门下存在员工，无法删除");
        }
        if (removeById(id)) {
            return Response.success();
        }
        return Response.error();
    }

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            Long childCount = this.deptMapper.selectCount(new QueryWrapper<Dept>().eq("parent_id", id));
            if (childCount > 0) {
                return Response.error("部门ID=" + id + "下存在子部门，无法删除");
            }
            Long staffCount = this.deptMapper.countStaffByDeptId(id);
            if (staffCount > 0) {
                return Response.error("部门ID=" + id + "下存在员工，无法删除");
            }
        }
        if (removeBatchByIds(ids)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO edit(Dept dept) {
        // 未传 parentId 时取库中原值来判断是否为子部门（update 会忽略 null 字段，不会覆盖原值）
        Integer parentId = dept.getParentId();
        if (parentId == null) {
            Dept db = getById(dept.getId());
            parentId = db == null ? 0 : db.getParentId();
        }
        // 子部门且传入了完整上下班时间时才重新计算总工时
        if (parentId != null && parentId != 0
                && dept.getMorStartTime() != null && dept.getMorEndTime() != null
                && dept.getAftStartTime() != null && dept.getAftEndTime() != null) {
            dept.setTotalWorkTime(calculateTotalWorkTime(dept));
        }
        QueryWrapper<Dept> queryWrapper = new QueryWrapper();
        queryWrapper.eq("id", dept.getId());
        if (update(dept, queryWrapper) || save(dept)) {
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 查找所有部门
     *
     * @return
     */
    public ResponseDTO queryAll() {
        List<Dept> list = list();
        // 为父级部门设置子部门
        return Response.success(setSubDept(list));
    }

    public ResponseDTO query(Integer id) {
        Dept dept = getById(id);
        if (dept != null) {
            return Response.success(dept);
        }
        return Response.error();
    }

    public ResponseDTO list(Integer current, Integer size, String name) {
        IPage<Dept> config = new Page<>(current, size);
        String searchName = (name == null) ? "" : name;
        IPage<Dept> page = this.deptMapper.listParentDept(config, searchName);
        List<Dept> parentList = page.getRecords();

        if (CollectionUtils.isEmpty(parentList)) {
            Map<String, Object> map = new HashMap<>();
            map.put("pages", page.getPages());
            map.put("total", page.getTotal());
            map.put("list", Collections.emptyList());
            return Response.success(map);
        }

        // 仅提取当前页父部门的 ID 列表
        List<Integer> parentIds = parentList.stream()
                .map(Dept::getId)
                .collect(Collectors.toList());

        // 按需仅查询当前页父部门下属的子部门
        List<Dept> subList = this.list(new LambdaQueryWrapper<Dept>()
                .in(Dept::getParentId, parentIds));

        // 按 parentId 分组 (O(M))
        Map<Integer, List<Dept>> subMap = subList.stream()
                .collect(Collectors.groupingBy(Dept::getParentId));

        // 组装子部门列表并防空
        for (Dept parentDept : parentList) {
            parentDept.setChildren(subMap.getOrDefault(parentDept.getId(), Collections.emptyList()));
        }

        Map<String, Object> map = new HashMap<>();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", parentList);
        return Response.success(map);
    }

    /**
     * 数据导出
     *
     * @param response
     * @return
     */
    public void export(HttpServletResponse response,String filename) throws Exception {
        List<Dept> list = this.deptMapper.queryAllSub();
        EasyExcelUtil.write(response, list, filename, Dept.class);
    }

    /**
     * 数据导入
     *
     * @param file
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO imp(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();
        List<Dept> list = EasyExcelUtil.read(inputStream, 1, Dept.class);
        // IService接口中的方法.批量插入数据
        if (saveBatch(list)) {
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 为父级部门设置子部门，使用流来处理数据，并返回父级部门
     *
     * @param list 所有部门列表
     * @return 树形结构后的父级部门列表
     */
    public List<Dept> setSubDept(List<Dept> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        // 按 parentId 分组
        Map<Integer, List<Dept>> subMap = list.stream()
                .filter(dept -> dept.getParentId() != null && !Objects.equals(dept.getParentId(), 0))
                .collect(Collectors.groupingBy(Dept::getParentId));

        // 筛选父级部门并组装子部门
        List<Dept> parentList = list.stream()
                .filter(dept -> Objects.equals(dept.getParentId(), 0))
                .collect(Collectors.toList());

        for (Dept parentDept : parentList) {
            parentDept.setChildren(subMap.getOrDefault(parentDept.getId(), Collections.emptyList()));
        }
        return parentList;
    }

    // 计算员工每天的上班时间
    public BigDecimal calculateTotalWorkTime(Dept dept) {
        long morDiff = dept.getMorEndTime().getTime() - dept.getMorStartTime().getTime();
        long aftDiff = dept.getAftEndTime().getTime() - dept.getAftStartTime().getTime();
        BigDecimal totalWorkTime = BigDecimal.valueOf((morDiff + aftDiff) / (1000 * 60 * 60.0)); // 此处除数一定要是浮点数类型
        return totalWorkTime;
    }
}
