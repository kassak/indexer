= Описание =
Библиотека indexer представляет собой сервис для фонового
индексирования файлов, для дальнейшего поиска. Ее внешний
интерфейсом является класс *Indexer*, на конструктор которому
передаются: фабрика классов, выполняющих разделение файлов
на слова, и размеры внутренних очередей, количество потоков
разбора файлов. После создания класса необходимо запустить
сервис методом *startService()* (методом *stopService()* его
можно остановить). Далее можно пользоваться методами класс:
 - *add()* асинхронная регистрация папки/файла;
 - *remove()* асинхронная дерегистрация папки/файла;
 - *search()* получение списка файлов и их актуальность по слову;
 - *getFiles()* получение списка всех файлов в индексе + доп.информация.

= Архитектура =

== Indexer ==
Сервис *Indexer* скрывает от пользователя внутреннюю структуру
библиотеки. Внутри себя он запускает сервисы *IndexManagerService*
и *FSWatcherService*.

== FSWatcherService ==
Сервис *FSWatcherService* запускает внутри себя *FSEventsService*,
а сам запускает поток, в котором обрабатывает запросы пользователя на
добавление/удаление папок/файлов. Асинхронная обработка этих запросов
необходима чтобы не блокировать пользовательский поток при обработке
директорий с большим числом файлов.

== FSEventsService ==
Сервис *FSEventsService* в своем потоке слушает события файловой системы,
при необходимости регистрирует/дерегистрирует подиректории зарегистрированных
дирректорий. События сообщаются сервису *IndexManagerService* через
интерфейс *IFSEventsProcessor*.

== IndexManagerService ==
Сервис *IndexManagerService* запускает внутри себя сревис *FilesProcessorService*,
а сам запускает поток, в котором занимается поддержанием индекса в актуальном
состоянии. Реализует интерфейс *IFSEventsProcessor* для получения уведомлений,
о событиях файловой системы от сервиса *FSEventsService*, *IFileProcessingResults*
для получения результатов обработки файлов от сервиса *FilesProcessorService* и
интерфейс *IFilesProcessor* для получения команд на обработку файлов от класса
*IndexProcessor*. События файловой системы и результаты обработки файлов
складываются в очередь, которую обрабатывает поток. Таким образом модификация
индекса происходит только в этом потоке, что позволяет уменьшить число блокировок
и упростить модификацию индекса. Обработка индекса делегируется классу *IndexProcessor*.

== IndexProcessor ==
Хранит внутри себя индекс *IndexStorage* и обрабатывает запросы на его
актуализацию и получение данных. Модификация данных происходит из одного потока.
Запросы на обработку файлов передаются сервису *IndexManagerService* через
интерфейс *IFilesProcessor*.

== IndexStorage ==
Класс хранящий в себе файлы с информаций об их статусе, слова и двунаправленные
связи между ними. Гарантированно Single-Writer Multiple-Reader.

== FilesProcessorService ==
Сервис, занимающийся обработкой файлов. Содержит в себе пул потоков, обрабатывающих
запросы на разбиение файлов, полученных через интерфейс *IFilesProcessor*. Добавление
файла в очередь не блокирующее. Результаты разбора идут в *IndexManagerService* через
интерфейс *IFileProcessingResults*.

== Потоки ==
Пользовательский поток блокируется только при заполнении очереди на добавление
(хотя можно сделать возможность отаказа). Потоки *FSWatcherService* и *FSEventsService*
могут производить большой объем данных, поэтому в случае заполнения очереди
*IndexManagerService*, эти потоки блокируются на отправке новых сообщений,
пока другие части системы не справятся со своей работой. Поток *IndexManagerService*
почти никогда не блокируется - он потребляет и перераспределяет почти все сообщения.
Потоки *FilesProcessorService* блокируются если результат разбора файлаов не
успевает обрабатываться. Сообщения этих потоков обрабатываются приоритетно.