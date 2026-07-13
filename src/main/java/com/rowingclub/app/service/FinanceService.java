package com.rowingclub.app.service;

import com.rowingclub.app.common.exception.BusinessException;
import com.rowingclub.app.common.exception.ResourceNotFoundException;
import com.rowingclub.app.dto.CreateExpenseRequest;
import com.rowingclub.app.dto.CreateIncomeRequest;
import com.rowingclub.app.dto.ExpenseResponse;
import com.rowingclub.app.dto.FinanceSummaryResponse;
import com.rowingclub.app.dto.IncomeItemResponse;
import com.rowingclub.app.dto.MonthlySummaryResponse;
import com.rowingclub.app.dto.YearlySummaryResponse;
import com.rowingclub.app.entity.Expense;
import com.rowingclub.app.entity.Income;
import com.rowingclub.app.entity.MembershipType;
import com.rowingclub.app.entity.Payment;
import com.rowingclub.app.entity.User;
import com.rowingclub.app.repository.ExpenseRepository;
import com.rowingclub.app.repository.IncomeRepository;
import com.rowingclub.app.repository.MembershipTypeRepository;
import com.rowingclub.app.repository.PaymentRepository;
import com.rowingclub.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Admin Gelir-Gider: Gelir = onaylanmış (SUCCESS) ödemeler + manuel gelirler; Gider = manuel kayıtlar. */
@Service
@RequiredArgsConstructor
public class FinanceService {

    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final MembershipTypeRepository membershipTypeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ---------- Gelir ----------

    /** Onaylı üyelik ödemeleri + manuel gelirler; tarihe göre en yeni önde. */
    @Transactional(readOnly = true)
    public List<IncomeItemResponse> getIncome(LocalDate start, LocalDate end) {
        List<IncomeItemResponse> result = new ArrayList<>();

        paymentRepository
                .findIncomeBetween(start.atStartOfDay(), end.atTime(LocalTime.MAX))
                .forEach(p -> result.add(toIncomeItem(p)));

        incomeRepository.findAllInRange(start, end)
                .forEach(i -> result.add(toIncomeItem(i)));

        result.sort(Comparator.comparing(IncomeItemResponse::getDate).reversed());
        return result;
    }

    @Transactional
    public IncomeItemResponse createIncome(CreateIncomeRequest request, UUID adminId) {
        Income.IncomeCategory category = parseIncomeCategory(request.getCategory());

        MembershipType branch = null;
        if (category == Income.IncomeCategory.SUBE) {
            if (request.getBranchTypeId() == null) {
                throw new BusinessException("Şube seçimi zorunlu");
            }
            branch = membershipTypeRepository.findById(request.getBranchTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "MembershipType", "id", request.getBranchTypeId()));
        }

        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new BusinessException("Geçerli bir tutar giriniz");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        Income income = Income.builder()
                .category(category)
                .branchType(branch)
                .description(request.getDescription())
                .amount(request.getAmount())
                .incomeDate(request.getIncomeDate() != null
                        ? request.getIncomeDate() : LocalDate.now())
                .createdBy(admin)
                .build();

        incomeRepository.save(income);
        return toIncomeItem(income);
    }

    /** Yalnızca manuel gelirler silinebilir; üyelik ödemeleri buradan silinmez. */
    @Transactional
    public void deleteIncome(UUID id) {
        if (!incomeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Income", "id", id);
        }
        incomeRepository.deleteById(id);
    }

    // ---------- Gider ----------

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(LocalDate start, LocalDate end) {
        return expenseRepository.findAllInRange(start, end)
                .stream()
                .map(this::toExpenseResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getTrainerPayments(UUID trainerId) {
        return expenseRepository.findAllByTrainerId(trainerId)
                .stream()
                .map(this::toExpenseResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getTrainerPayments(UUID trainerId, LocalDate start, LocalDate end) {
        return expenseRepository.findAllByTrainerIdInRange(trainerId, start, end)
                .stream()
                .map(this::toExpenseResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request, UUID adminId) {
        Expense.ExpenseCategory category = parseCategory(request.getCategory());

        MembershipType branch = null;
        User trainer = null;

        if (category == Expense.ExpenseCategory.SUBE) {
            if (request.getBranchTypeId() == null) {
                throw new BusinessException("Şube seçimi zorunlu");
            }
            branch = membershipTypeRepository.findById(request.getBranchTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "MembershipType", "id", request.getBranchTypeId()));
        } else if (category == Expense.ExpenseCategory.PERSONEL) {
            if (request.getTrainerId() == null) {
                throw new BusinessException("Personel seçimi zorunlu");
            }
            trainer = userRepository.findById(request.getTrainerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User", "id", request.getTrainerId()));
        }

        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new BusinessException("Geçerli bir tutar giriniz");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        Expense expense = Expense.builder()
                .category(category)
                .branchType(branch)
                .trainer(trainer)
                .description(request.getDescription())
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate() != null
                        ? request.getExpenseDate() : LocalDate.now())
                .createdBy(admin)
                .build();

        expenseRepository.save(expense);
        return toExpenseResponse(expense);
    }

    /** Antrenör ödeme sayfasından çağrılır: kategori PERSONEL sabittir. */
    @Transactional
    public ExpenseResponse createTrainerPayment(
            UUID trainerId, CreateExpenseRequest request, UUID adminId) {
        request.setCategory(Expense.ExpenseCategory.PERSONEL.name());
        request.setTrainerId(trainerId);
        ExpenseResponse response = createExpense(request, adminId);
        userRepository.findById(trainerId)
                .ifPresent(trainer -> notificationService.sendTrainerPaymentMade(trainer, request.getAmount()));
        return response;
    }

    @Transactional
    public void deleteExpense(UUID id) {
        if (!expenseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Expense", "id", id);
        }
        expenseRepository.deleteById(id);
    }

    // ---------- Özet ----------

    @Transactional(readOnly = true)
    public FinanceSummaryResponse getSummary(LocalDate start, LocalDate end) {
        BigDecimal income = getIncome(start, end).stream()
                .map(IncomeItemResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = getExpenses(start, end).stream()
                .map(ExpenseResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return FinanceSummaryResponse.builder()
                .startDate(start)
                .endDate(end)
                .totalIncome(income)
                .totalExpense(expense)
                .net(income.subtract(expense))
                .build();
    }

    // ---------- Yıllık ----------

    /**
     * Bir yılın 12 ayı için gelir/gider/net kırılımı. Ay-ay ayrı sorgu atmak
     * yerine yılın tamamını 3 sorguda çekip Java tarafında ay bazında toplar.
     */
    @Transactional(readOnly = true)
    public YearlySummaryResponse getYearlySummary(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        BigDecimal[] incomeByMonth = new BigDecimal[12];
        BigDecimal[] expenseByMonth = new BigDecimal[12];
        for (int i = 0; i < 12; i++) {
            incomeByMonth[i] = BigDecimal.ZERO;
            expenseByMonth[i] = BigDecimal.ZERO;
        }

        for (Payment p : paymentRepository.findIncomeBetween(
                start.atStartOfDay(), end.atTime(LocalTime.MAX))) {
            int idx = p.getCreatedAt().getMonthValue() - 1;
            incomeByMonth[idx] = incomeByMonth[idx].add(p.getAmount());
        }
        for (Income i : incomeRepository.findAllInRange(start, end)) {
            int idx = i.getIncomeDate().getMonthValue() - 1;
            incomeByMonth[idx] = incomeByMonth[idx].add(i.getAmount());
        }
        for (Expense e : expenseRepository.findAllInRange(start, end)) {
            int idx = e.getExpenseDate().getMonthValue() - 1;
            expenseByMonth[idx] = expenseByMonth[idx].add(e.getAmount());
        }

        List<MonthlySummaryResponse> months = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        for (int i = 0; i < 12; i++) {
            BigDecimal inc = incomeByMonth[i];
            BigDecimal exp = expenseByMonth[i];
            totalIncome = totalIncome.add(inc);
            totalExpense = totalExpense.add(exp);
            months.add(MonthlySummaryResponse.builder()
                    .month(i + 1)
                    .totalIncome(inc)
                    .totalExpense(exp)
                    .net(inc.subtract(exp))
                    .build());
        }

        return YearlySummaryResponse.builder()
                .year(year)
                .months(months)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .net(totalIncome.subtract(totalExpense))
                .build();
    }

    // ---------- Yardımcılar ----------

    private Expense.ExpenseCategory parseCategory(String raw) {
        try {
            return Expense.ExpenseCategory.valueOf(raw);
        } catch (Exception e) {
            throw new BusinessException("Geçersiz kategori", HttpStatus.BAD_REQUEST);
        }
    }

    private Income.IncomeCategory parseIncomeCategory(String raw) {
        try {
            return Income.IncomeCategory.valueOf(raw);
        } catch (Exception e) {
            throw new BusinessException("Geçersiz kategori", HttpStatus.BAD_REQUEST);
        }
    }

    private IncomeItemResponse toIncomeItem(Payment p) {
        String branchLabel = p.getPlan().getPlanTypes().stream()
                .map(pt -> pt.getMembershipType().getName())
                .distinct()
                .collect(java.util.stream.Collectors.joining(" + "));
        if (branchLabel.isBlank()) {
            branchLabel = "Diğer";
        }
        return IncomeItemResponse.builder()
                .id(p.getId())
                .type("UYELIK")
                .title(p.getUser().getFullName())
                .subtitle(p.getPlan().getName() + " · "
                        + methodLabel(p.getPaymentMethod().name()))
                .amount(p.getAmount())
                .date(p.getCreatedAt().toLocalDate())
                .branchLabel(branchLabel)
                .build();
    }

    private IncomeItemResponse toIncomeItem(Income i) {
        String title = i.getCategory() == Income.IncomeCategory.SUBE
                ? (i.getBranchType() != null ? i.getBranchType().getName() : "Şube")
                : "Diğer";
        String branchLabel = i.getCategory() == Income.IncomeCategory.SUBE
                ? (i.getBranchType() != null ? i.getBranchType().getName() : "Diğer")
                : "Diğer";
        return IncomeItemResponse.builder()
                .id(i.getId())
                .type("MANUEL")
                .title(title)
                .subtitle(i.getDescription())
                .amount(i.getAmount())
                .date(i.getIncomeDate())
                .branchLabel(branchLabel)
                .build();
    }

    private String methodLabel(String method) {
        return switch (method) {
            case "EFT" -> "EFT";
            case "IYZICO" -> "Online";
            default -> "Nakit";
        };
    }

    private ExpenseResponse toExpenseResponse(Expense e) {
        return ExpenseResponse.builder()
                .id(e.getId())
                .category(e.getCategory().name())
                .branchTypeId(e.getBranchType() != null ? e.getBranchType().getId() : null)
                .branchTypeName(e.getBranchType() != null ? e.getBranchType().getName() : null)
                .trainerId(e.getTrainer() != null ? e.getTrainer().getId() : null)
                .trainerName(e.getTrainer() != null ? e.getTrainer().getFullName() : null)
                .description(e.getDescription())
                .amount(e.getAmount())
                .expenseDate(e.getExpenseDate())
                .createdByName(e.getCreatedBy().getFullName())
                .createdAt(e.getCreatedAt())
                .build();
    }
}