package by.masarnovsky


const val REPAY_VALUE = "Возврат суммы"
const val MAIN_MENU_DESCRIPTION = "Выберите валюту, в которой будут хранится долги (BYN по-умолчанию)"
const val UNKNOWN_REQUEST = "Не могу обработать запрос.\nОзнакомьтесь с командами: /howto"
const val CURRENT_CURRENCY = "Текущая валюта: %s"
const val NEGATIVE_BALANCE_ERROR = "Введена неверная сумма, баланс не может быть отрицательным"
const val COMMON_ERROR = "Произошла ошибка. Пожалуйста, повторите запрос"
const val NOT_DELETE_HISTORY = "Вы решили не удалять историю"
const val DEBTOR_RECORD = "Долг %s равен <b>%.2f</b> %s за: <i>%s</i>\n<i>Посмотреть всех:</i> /all"
const val DEBTOR_RETURN_RECORD = "%s вернул(а) <b>%.2f</b> %s и теперь"
const val DEBTOR_ZERO_CREDIT = " ничего не должен(на)"
const val DEBTOR_CREDIT_AFTER_REPAY = " должен(на) тебе <b>%.2f</b> %s за: <i>%s</i>"
const val DEBTOR_SUGGESTION_FOR_INLINE_QUERY = "Долг %s равен %.2f %s"
const val DEBTOR_RECORD_FOR_INLINE_QUERY = "Долг %s равен <b>%.2f</b> %s за: <i>%s</i>"
const val DEBTOR_RECORD_SHORT = "%s <b>%.2f</b> %s за: <i>%s</i>"
const val SUCCESSFUL_DEBTOR_REMOVAL = "Информация о должнике %s была удалена"
const val SUCCESSFUL_DEBTORS_REMOVAL = "Информация о %d должниках была удалена"
const val DEBTOR_NOT_FOUND = "По такому имени ничего не найдено"
const val WRONG_COMMAND_FORMAT = "Неверный формат команды"
const val MERGE_DEBTOR_NOT_FOUND = "Невозможно найти кого-то из [%s, %s]. Доступные имена:\n%s"
const val MERGE_DEBTOR_DUPLICATE_ERROR = "Нельзя скопировать в того же должника"
const val MERGE_DEBTOR_SUCCESS = "Скопировано %d транзакций из %s в %s"
const val DEBTORS_NOT_FOUND = "Должников не найдено"
const val DELETE_ALL_DEBTORS_WARNING = "Вы точно хотите удалить <b>всех</b> должников?"
const val DEBTOR_LOG_HISTORY_HEADER = "Текущий долг для %s равняется <b>%.2f</b> %s\nИстория долгов:\n"
const val LOG_SUMMARIZE = "%s |   <b>%.2f</b> за %s\n"
const val YES = "Да"
const val NO = "Нет"
const val SHOW_MERGED_DEBTOR_BUTTON = "Показать долги %s"
const val CURRENT_DEBTS_TOTAL_AMOUNT = "Общая сумма долгов равна <b>%.2f</b> %s\n"
const val DEBTOR_HISTORICAL_CREDIT = "За все время вы одолжили %s <b>%.2f</b> %s\n"
const val ADMIN_MERGE_SUCCESS = "Пользователь %d.\nМигрировано %d транзакций должнику %s. %s был удален."
const val HOWTO_INFO = """
Добавляй должника в таком формате: 
<b>имя 66.6 комментарий</b> 
Чтобы вычесть сумму долга: 
<b>имя -97</b>    
    
Список доступных комманд:
Показать историю для конкретного должника:
<b>/show имя</b>
Без параметров будет выведен список всех должников:
<b>/show</b>
Список всех должников:
<b>/all</b>
Удалить запись о должнике:
<b>/delete имя</b> 
Без параметров будут удалены <b>все</b> должники:
<b>/delete</b> 
Если нужно перенести данные от одного должника к другому, используй:
<b>/merge имя_источника имя_получателя</b>
"""



