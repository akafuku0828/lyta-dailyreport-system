package com.techacademy.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.techacademy.constants.ErrorKinds;
import com.techacademy.constants.ErrorMessage;

import com.techacademy.entity.Employee;
import com.techacademy.entity.Employee.Role;
import com.techacademy.entity.Report;
import com.techacademy.service.ReportService;
import com.techacademy.service.UserDetail;

@Controller
@RequestMapping("reports")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // 日報一覧画面
    @GetMapping
    public String list(@AuthenticationPrincipal UserDetail userDetail, Model model, Employee employee) {

        employee = userDetail.getEmployee();
        //ログインユーザーの権限が一般の場合は自分の日報のみを表示
        Role role = userDetail.getEmployee().getRole();
        if (role.toString() == "GENERAL") {
            model.addAttribute("listSize", reportService.findAll(employee).size());
            model.addAttribute("reportList", reportService.findAll(employee));
            return "reports/list";
        }

        model.addAttribute("listSize", reportService.findAll().size());
        model.addAttribute("reportList", reportService.findAll());

        return "reports/list";
    }

    // 日報詳細画面
    @GetMapping(value = "/{ID}/")
    public String detail(@PathVariable("ID") Integer id, Model model) {

        model.addAttribute("report", reportService.findById(id));
        model.addAttribute("employeeName", reportService.findById(id).getEmployee().getName());
        return "reports/detail";
    }

    // 日報更新画面
    @GetMapping(value = "/{ID}/update")
    public String edit(@PathVariable("ID") Integer id, Model model, @ModelAttribute Report report) {
        model.addAttribute("employeeName", reportService.findById(id).getEmployee().getName());
        report.setReportDate(reportService.findById(id).getReportDate());
        report.setTitle(reportService.findById(id).getTitle());
        report.setContent(reportService.findById(id).getContent());
        report.setId(id);
        return "reports/update";
    }

    // 日報更新処理
    @PostMapping(value = "/{ID}/update")
    public String update(@Validated Report report, BindingResult res, Model model, @PathVariable("ID") Integer id, @AuthenticationPrincipal UserDetail userDetail) {

        report.setCreatedAt(reportService.findById(id).getCreatedAt());

        if (res.hasErrors()) {
            return edit(id,model,report);
        }

        //ログイン中の従業員で、DBに同じ日付が存在するかチェック(画面で表示中のデータを除いたものに限る)
        if(!report.getReportDate().equals(reportService.findById(id).getReportDate())) {

            for (Report dbReport : reportService.findAll(userDetail.getEmployee())) {
                LocalDate dbDate = dbReport.getReportDate();

                if (report.getReportDate().isEqual(dbDate)) {
                    model.addAttribute(ErrorMessage.getErrorName(ErrorKinds.DATECHECK_ERROR),
                            ErrorMessage.getErrorValue(ErrorKinds.DATECHECK_ERROR));
                    return edit(id,model,report);
                }
            }
        }

        report.setEmployee(reportService.findById(id).getEmployee());
        report.setId(reportService.findById(id).getId());

        ErrorKinds result = reportService.save(report, userDetail);

        if (ErrorMessage.contains(result)) {
            model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
            return edit(id,model,report);
            }

        return "redirect:/reports";
    }

    // 日報新規登録画面
    @GetMapping(value = "/add")
    public String create(@ModelAttribute Report report,Model model, @AuthenticationPrincipal UserDetail userDetail) {
        model.addAttribute("employeeName", userDetail.getEmployee().getName());
        return "reports/new";
    }

    // 日報新規登録処理
    @PostMapping(value = "/add")
    public String add(@Validated Report report, BindingResult res, Model model,@AuthenticationPrincipal UserDetail userDetail) {

        report.setEmployee(userDetail.getEmployee());
        LocalDateTime now = LocalDateTime.now();
        report.setCreatedAt(now);

        if (res.hasErrors()) {
            return create(report,model,userDetail);
        }

        //ログイン中の従業員で、DBに同じ日付が存在するかチェック
        for (Report dbReport : reportService.findAll(userDetail.getEmployee())) {
            LocalDate dbDate = dbReport.getReportDate();

            if (report.getReportDate().isEqual(dbDate)) {
                model.addAttribute(ErrorMessage.getErrorName(ErrorKinds.DATECHECK_ERROR),
                        ErrorMessage.getErrorValue(ErrorKinds.DATECHECK_ERROR));
                return create(report,model,userDetail);
            }
        }

        ErrorKinds result = reportService.save(report, userDetail);

        if (ErrorMessage.contains(result)) {
            model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
            return create(report,model,userDetail);
        }

        return  "redirect:/reports";
    }

    // 日報削除処理
    @PostMapping(value = "/{ID}/delete")
    public String delete(@PathVariable("ID") Integer id, @AuthenticationPrincipal UserDetail userDetail, Model model) {

        ErrorKinds result = reportService.delete(id);

        if (ErrorMessage.contains(result)) {
            model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
            model.addAttribute("report", reportService.findById(id));
            return detail(id, model);
        }

        return "redirect:/reports";
    }

}
