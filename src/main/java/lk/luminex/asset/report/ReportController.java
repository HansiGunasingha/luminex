package lk.luminex.asset.report;

import lk.luminex.asset.common_asset.model.NameCount;
import lk.luminex.asset.common_asset.model.ParameterCount;
import lk.luminex.asset.common_asset.model.TwoDate;
import lk.luminex.asset.employee.entity.Employee;
import lk.luminex.asset.item.service.ItemService;
import lk.luminex.asset.project.entity.Project;
import lk.luminex.asset.project.service.ProjectService;
import lk.luminex.asset.project_order.entity.ProjectOrder;
import lk.luminex.asset.payment.entity.enums.PaymentMethod;
import lk.luminex.asset.project_order.entity.enums.OrderState;
import lk.luminex.asset.project_order.service.ProjectOrderService;
import lk.luminex.asset.order_ledger.entity.OrderLedger;
import lk.luminex.asset.order_ledger.service.OrderLedgerService;
import lk.luminex.asset.item.entity.Item;
import lk.luminex.asset.payment.entity.Payment;
import lk.luminex.asset.payment.service.PaymentService;
import lk.luminex.asset.report.model.ProjectSupplierItem;
import lk.luminex.asset.supplier_item.entity.SupplierItem;
import lk.luminex.asset.supplier_item.service.SupplierItemService;
import lk.luminex.asset.user_management.user.service.UserService;
import lk.luminex.asset.viva.entity.SupplierItemObj;
import lk.luminex.util.service.DateTimeAgeService;
import lk.luminex.util.service.OperatorService;
import lombok.NonNull;
import org.hibernate.mapping.Collection;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/report")
public class ReportController {
  private final PaymentService paymentService;
  private final ProjectOrderService projectOrderService;
  private final OperatorService operatorService;
  private final DateTimeAgeService dateTimeAgeService;
  private final UserService userService;
  private final OrderLedgerService orderLedgerService;
  private final ProjectService projectService;
  private final ItemService itemService;
  private final SupplierItemService supplierItemService;

  public ReportController(PaymentService paymentService, ProjectOrderService projectOrderService,
                          OperatorService operatorService, DateTimeAgeService dateTimeAgeService,
                          UserService userService, OrderLedgerService orderLedgerService,
                          ProjectService projectService, ItemService itemService,
                          SupplierItemService supplierItemService) {
    this.paymentService = paymentService;
    this.projectOrderService = projectOrderService;
    this.operatorService = operatorService;
    this.dateTimeAgeService = dateTimeAgeService;
    this.userService = userService;
    this.orderLedgerService = orderLedgerService;
    this.projectService = projectService;
    this.itemService = itemService;
    this.supplierItemService = supplierItemService;
  }

  private String commonAll(List< Payment > payments, List< ProjectOrder > projectOrders, Model model, String message,
                           LocalDateTime startDateTime, LocalDateTime endDateTime) {
    //according to payment type -> order
    commonOrders(projectOrders, model);
    //according to payment type -> payment
    commonPayment(payments, model);
    // order count by cashier
    commonPerCashier(projectOrders, model);
    // payment count by account department
    commonPerAccountUser(payments, model);
    // item count according to item
    commonPerItem(startDateTime, endDateTime, model);

    model.addAttribute("message", message);
    return "report/paymentAndIncomeReport";
  }

  @GetMapping( "/manager" )
  public String getAllOrderAndPayment(Model model) {
    LocalDate localDate = LocalDate.now();
    String message = "This report is belongs to " + localDate.toString();
    LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(localDate);
    LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(localDate);

    return commonAll(paymentService.findByCreatedAtIsBetween(startDateTime, endDateTime),
                     projectOrderService.findByCreatedAtIsBetween(startDateTime, endDateTime), model, message,
                     startDateTime, endDateTime);

  }

  @PostMapping( "/manager/search" )
  public String getAllOrderAndPaymentBetweenTwoDate(@ModelAttribute( "twoDate" ) TwoDate twoDate, Model model) {
    String message =
        "This report is between from " + twoDate.getStartDate().toString() + " to " + twoDate.getEndDate().toString();
    LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(twoDate.getStartDate());
    LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(twoDate.getEndDate());
    return commonAll(paymentService.findByCreatedAtIsBetween(startDateTime, endDateTime),
                     projectOrderService.findByCreatedAtIsBetween(startDateTime, endDateTime), model, message,
                     startDateTime, endDateTime);
  }

  private void commonOrders(List< ProjectOrder > projectOrders, Model model) {
    // order count
    int orderTotalCount = projectOrders.size();
    model.addAttribute("orderTotalCount", orderTotalCount);


  }

  @GetMapping( "/cashier" )
  public String getCashierToday(Model model) {
    LocalDate localDate = LocalDate.now();
    String message = "This report is belongs to " + localDate.toString() + " and \n congratulation all are done by " +
        "you.";
    LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(localDate);
    LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(localDate);
    commonOrders(projectOrderService.findByCreatedAtIsBetweenAndCreatedBy(startDateTime, endDateTime,
                                                                          SecurityContextHolder.getContext().getAuthentication().getName()), model);
    model.addAttribute("message", message);
    return "report/cashierReport";
  }

  @PostMapping( "/cashier/search" )
  public String getCashierSearch(@ModelAttribute( "twoDate" ) TwoDate twoDate, Model model) {
    String message =
        "This report is between from " + twoDate.getStartDate().toString() + " to " + twoDate.getEndDate().toString() + " and \n congratulation all are done by you.";
    LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(twoDate.getStartDate());
    LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(twoDate.getEndDate());
    commonOrders(projectOrderService.findByCreatedAtIsBetweenAndCreatedBy(startDateTime, endDateTime,
                                                                          SecurityContextHolder.getContext().getAuthentication().getName()), model);
    model.addAttribute("message", message);
    return "report/cashierReport";
  }

  private void commonPayment(List< Payment > payments, Model model) {
    // payment count
    int paymentTotalCount = payments.size();
    model.addAttribute("paymentTotalCount", paymentTotalCount);
    //|-> card
    List< Payment > paymentCards =
        payments.stream().filter(x -> x.getPaymentMethod().equals(PaymentMethod.CREDIT)).collect(Collectors.toList());
    int paymentCardCount = paymentCards.size();
    AtomicReference< BigDecimal > paymentCardAmount = new AtomicReference<>(BigDecimal.ZERO);
    paymentCards.forEach(x -> {
      BigDecimal addAmount = operatorService.addition(paymentCardAmount.get(), x.getAmount());
      paymentCardAmount.set(addAmount);
    });
    model.addAttribute("paymentCardCount", paymentCardCount);
    model.addAttribute("paymentCardAmount", paymentCardAmount.get());
    //|-> cash
    List< Payment > paymentCash =
        payments.stream().filter(x -> x.getPaymentMethod().equals(PaymentMethod.CASH)).collect(Collectors.toList());
    int paymentCashCount = paymentCash.size();
    AtomicReference< BigDecimal > paymentCashAmount = new AtomicReference<>(BigDecimal.ZERO);
    paymentCash.forEach(x -> {
      BigDecimal addAmount = operatorService.addition(paymentCashAmount.get(), x.getAmount());
      paymentCashAmount.set(addAmount);
    });
    model.addAttribute("paymentCashCount", paymentCashCount);
    model.addAttribute("paymentCardAmount", paymentCashAmount.get());

  }

  @GetMapping( "/payment" )
  public String getPaymentToday(Model model) {
    LocalDate localDate = LocalDate.now();
    String message = "This report is belongs to " + localDate.toString() + " and \n congratulation all are done by " +
        "you.";
    LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(localDate);
    LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(localDate);
    commonPayment(paymentService.findByCreatedAtIsBetweenAndCreatedBy(startDateTime, endDateTime,
                                                                      SecurityContextHolder.getContext().getAuthentication().getName()), model);
    model.addAttribute("message", message);
    return "report/paymentReport";
  }

  @PostMapping( "/payment/search" )
  public String getPaymentSearch(@ModelAttribute( "twoDate" ) TwoDate twoDate, Model model) {
    String message =
        "This report is between from " + twoDate.getStartDate().toString() + " to " + twoDate.getEndDate().toString() + " and \n congratulation all are done by you.";
    LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(twoDate.getStartDate());
    LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(twoDate.getEndDate());
    commonPayment(paymentService.findByCreatedAtIsBetweenAndCreatedBy(startDateTime, endDateTime,
                                                                      SecurityContextHolder.getContext().getAuthentication().getName()), model);
    model.addAttribute("message", message);
    return "report/paymentReport";
  }

  private void commonPerCashier(List< ProjectOrder > projectOrders, Model model) {
    List< NameCount > orderByCashierAndTotalAmount = new ArrayList<>();
//name, count, total
        HashSet<String> createdByAll = new HashSet<>();
        projectOrders.forEach(x -> createdByAll.add(x.getCreatedBy()));

        createdByAll.forEach(x -> {
            NameCount nameCount = new NameCount();
            Employee employee = userService.findByUserName(x).getEmployee();
            nameCount.setName(employee.getTitle().getTitle() + " " + employee.getName());
            AtomicReference<BigDecimal> cashierTotalCount = new AtomicReference<>(BigDecimal.ZERO);
            List<ProjectOrder> cashierOrder =
                    projectOrders.stream().filter(a -> a.getCreatedBy().equals(x)).collect(Collectors.toList());
            nameCount.setCount(cashierOrder.size());
            nameCount.setTotal(cashierTotalCount.get());
            orderByCashierAndTotalAmount.add(nameCount);
        });
        model.addAttribute("orderByCashierAndTotalAmount", orderByCashierAndTotalAmount);
    }

    @GetMapping("/perCashier")
    public String perCashierToday(Model model) {
        LocalDate localDate = LocalDate.now();
        String message = "This report is belongs to " + localDate.toString();
        LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(localDate);
        LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(localDate);
        commonPerCashier(projectOrderService.findByCreatedAtIsBetween(startDateTime, endDateTime), model);
        model.addAttribute("message", message);
        return "report/perCashierReport";
    }

    @PostMapping("/perCashier/search")
    public String getPerCashierSearch(@ModelAttribute("twoDate") TwoDate twoDate, Model model) {
        String message =
                "This report is between from " + twoDate.getStartDate().toString() + " to " + twoDate.getEndDate().toString() + " and \n congratulation all are done by you.";
        LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(twoDate.getStartDate());
        LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(twoDate.getEndDate());
        commonPerCashier(projectOrderService.findByCreatedAtIsBetween(startDateTime, endDateTime), model);
        model.addAttribute("message", message);
        return "report/perCashierReport";
    }

    private void commonPerAccountUser(List<Payment> payments, Model model) {
        List<NameCount> paymentByUserAndTotalAmount = new ArrayList<>();
//name, count, total
    HashSet< String > createdByAllPayment = new HashSet<>();
    payments.forEach(x -> createdByAllPayment.add(x.getCreatedBy()));

    createdByAllPayment.forEach(x -> {
      NameCount nameCount = new NameCount();
      Employee employee = userService.findByUserName(x).getEmployee();
      nameCount.setName(employee.getTitle().getTitle() + " " + employee.getName());
      List< BigDecimal > userTotalCount = new ArrayList<>();
      List< Payment > paymentUser =
          payments.stream().filter(a -> a.getCreatedBy().equals(x)).collect(Collectors.toList());
      nameCount.setCount(paymentUser.size());
      paymentUser.forEach(a -> {
        userTotalCount.add(a.getAmount());
      });
      nameCount.setTotal(userTotalCount.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
      paymentByUserAndTotalAmount.add(nameCount);
    });

    model.addAttribute("paymentByUserAndTotalAmount", paymentByUserAndTotalAmount);

  }

  @GetMapping( "/perAccount" )
  public String perAccountToday(Model model) {
    LocalDate localDate = LocalDate.now();
    String message = "This report is belongs to " + localDate.toString();
    LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(localDate);
    LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(localDate);
    commonPerAccountUser(paymentService.findByCreatedAtIsBetween(startDateTime, endDateTime), model);
    model.addAttribute("message", message);
    return "report/perAccountReport";
  }

  @PostMapping( "/perAccount/search" )
  public String getPerAccountSearch(@ModelAttribute( "twoDate" ) TwoDate twoDate, Model model) {
    String message =
        "This report is between from " + twoDate.getStartDate().toString() + " to " + twoDate.getEndDate().toString() + " and \n congratulation all are done by you.";
    LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(twoDate.getStartDate());
    LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(twoDate.getEndDate());
    commonPerAccountUser(paymentService.findByCreatedAtIsBetween(startDateTime, endDateTime), model);
    model.addAttribute("message", message);
    return "report/perAccountReport";
  }

  private void commonPerItem(LocalDateTime startDateTime, LocalDateTime endDateTime, Model model) {
    HashSet< Item > orderItems = new HashSet<>();

    List< ParameterCount > itemNameAndItemCount = new ArrayList<>();

    List< OrderLedger > orderLedgers = orderLedgerService.findByCreatedAtIsBetween(startDateTime, endDateTime);
    orderLedgers.forEach(x -> orderItems.add(x.getLedger().getItem()));

    orderItems.forEach(x -> {
      ParameterCount parameterCount = new ParameterCount();
      parameterCount.setName(x.getName());
      parameterCount.setCount((int) orderLedgers
          .stream()
          .filter(a -> a.getLedger().getItem().equals(x))
          .count());
      itemNameAndItemCount.add(parameterCount);
    });
    model.addAttribute("itemNameAndItemCount", itemNameAndItemCount);

  }

  @GetMapping( "/perItem" )
  public String perItemToday(Model model) {
    LocalDate localDate = LocalDate.now();
    String message = "This report is belongs to " + localDate.toString();
    LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(localDate);
    LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(localDate);
    commonPerItem(startDateTime, endDateTime, model);
    model.addAttribute("message", message);
    return "report/perItemReport";
  }

  @PostMapping( "/perItem/search" )
  public String getPerItemSearch(@ModelAttribute( "twoDate" ) TwoDate twoDate, Model model) {
    String message =
        "This report is between from " + twoDate.getStartDate().toString() + " to " + twoDate.getEndDate().toString() + " and \n congratulation all are done by you.";
    LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(twoDate.getStartDate());
    LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(twoDate.getEndDate());
    commonPerItem(startDateTime, endDateTime, model);
    model.addAttribute("message", message);
    return "report/perItemReport";
  }

  @GetMapping( "/project" )
  public String perProject(Model model) {
    LocalDate localDate = LocalDate.now();
    String message = "This report is belongs to " + localDate.toString();
    LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(localDate);
    LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(localDate);
    commonProject(startDateTime, endDateTime, model);
    model.addAttribute("message", message);
    return "report/project";
  }

  @PostMapping( "/project" )
  public String getProject(@ModelAttribute( "twoDate" ) TwoDate twoDate, Model model) {
    String message =
        "This report is between from " + twoDate.getStartDate().toString() + " to " + twoDate.getEndDate().toString() + " and \n congratulation all are done by you.";
    LocalDateTime startDateTime = dateTimeAgeService.dateTimeToLocalDateStartInDay(twoDate.getStartDate());
    LocalDateTime endDateTime = dateTimeAgeService.dateTimeToLocalDateEndInDay(twoDate.getEndDate());
    commonProject(startDateTime, endDateTime, model);
    model.addAttribute("message", message);
    return "report/project";
  }

  private void commonProject(LocalDateTime startDateTime, LocalDateTime endDateTime, Model model) {
    List< ProjectSupplierItem > projectSupplierItems = new ArrayList<>();

    List< OrderLedger > orderLedgers = orderLedgerService.findByCreatedAtIsBetween(startDateTime, endDateTime);

    List< Project > projects = projectService.findAll();

    projects.forEach(x -> {
      for ( OrderLedger orderLedger : orderLedgers ) {
        if ( x.equals(orderLedger.getProjectOrder().getProject()) ) {
          ProjectSupplierItem projectSupplierItem = new ProjectSupplierItem();
          projectSupplierItem.setProject(projectService.findById(x.getId()));
          List< Item > projectItems = new ArrayList<>();

          List< ProjectOrder > projectOrders = new ArrayList<>();

          List< OrderLedger > orderLedgerPerProjects =
              orderLedgers
                  .stream()
                  .filter(y -> y.getProjectOrder().getProject().equals(x))
                  .collect(Collectors.toList());

          orderLedgerPerProjects
              .forEach(z -> projectOrders.add(projectOrderService.findById(z.getProjectOrder().getId())));

          List< ProjectOrder > activeProjectOrders =
              projectOrders.stream().filter(p -> p.getOrderState().equals(OrderState.APPROVED)).collect(Collectors.toList());


          for ( ProjectOrder activeProjectOrder : activeProjectOrders ) {
            activeProjectOrders.forEach(z -> {
              for ( OrderLedger ledger : z.getOrderLedgers() ) {
                activeProjectOrder.getOrderLedgers().forEach(s -> {
                  s.getProjectOrder().getOrderLedgers().forEach(q -> {
                    projectItems.add(itemService.findById(q.getId()));
                  });
                });


              }
            });
          }

          projectItems.stream().distinct();


          List< Item > projectItemWithDetails = new ArrayList<>();
          for ( Item projectItem : projectItems ) {
            List< SupplierItem > supplierItems = supplierItemService.findByItemOne(projectItem);
            if ( !supplierItems.isEmpty() ) {
              projectItem.setAmount(supplierItems.get(supplierItems.size() - 1).getPrice());
              int itemcounter = 0;
              for ( ProjectOrder activeProjectOrder : activeProjectOrders ) {
                if ( activeProjectOrder.getProject().equals(x) ) {
                  List< OrderLedger > orderLedgers1 =
                      activeProjectOrder.getOrderLedgers().stream().filter(v -> v.getLedger().getItem().equals(projectItem)).collect(Collectors.toList());
                  for ( OrderLedger ledger : orderLedgers1 ) {
                    itemcounter =
                        itemcounter + Integer.parseInt(orderLedgerService.findById(ledger.getId()).getQuantity());
                  }
                }
              }
              projectItem.setCount(itemcounter);
              projectItemWithDetails.add(projectItem);
            }


          }

          projectSupplierItem.setItems(projectItemWithDetails);
          projectSupplierItems.add(projectSupplierItem);

        }
      }
    });

    model.addAttribute("projectSupplierItems", projectSupplierItems);
  }

//sample wsa error

}
