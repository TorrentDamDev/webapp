package pages

import com.github.lavrov.bittorrent.app.protocol.Event
import com.raquo.laminar.api.L._
import default.Routing
import squants.experimental.formatter.Formatters.InformationMetricFormatter
import squants.information.Bytes


object SearchPage {

  def apply(query: Signal[Option[String]], request: String => Unit, results: Signal[Option[Event.SearchResults]]) = {

    val searchTermVar = Var(initial = "")

    val resultSignal = query.combineWith(results)

    val isLoading = resultSignal.map {
      case (Some(query), Some(results)) => query != results.query
      case (Some(_), None) => true
      case _ => false
    }

    section(cls := "section",
      div(
        onMountCallback { ctx =>
          query.foreach {
            case Some(v) => request(v)
            case None =>
          }(ctx.owner)
        },
        cls := "container is-max-desktop",
        form(cls := "block",
          onSubmit.preventDefault --> { _ =>
            val value = PartialFunction.condOpt(searchTermVar.now()) {
              case str if str.nonEmpty => str
            }
            Routing.router.pushState(Routing.Page.Root(value))
          },
          div(cls := "field has-addons",
            div(cls := "control is-large is-expanded", cls.toggle("is-loading") <-- isLoading,
              input(
                cls := "input is-primary is-large",
                typ := "text",
                placeholder := "Search",
                value <-- query.map(_.getOrElse("")),
                onInput.mapToValue --> searchTermVar
              )
            ),
            div(cls := "control is-hidden-mobile",
              button(cls := "button is-primary is-large", "Go")
            )
          )
        ),
        children <-- resultSignal
          .map {
            case (Some(_), Some(results)) =>
              results.entries.map { entry =>
                div(cls := "media",
                  div(cls := "media-content",
                    div(cls := "content",
                      p(cls := "subtitle is-6",
                        a(
                          onClick --> {_ => Routing.router.pushState(Routing.Page.Torrent(entry.infoHash))},
                          entry.name
                        )
                      ),
                      p(cls := "level is-mobile",
                        div(cls := "level-left",
                          div(cls := "level-item",
                            span(cls := "is-size-7 has-text-weight-light",
                              InformationMetricFormatter.inBestUnit(Bytes(entry.size)).rounded(1).toString
                            )
                          ),
                          div(cls := "level-item",
                            span(cls := "tags m-0",
                              entry.ext.map( ext =>
                                span(cls := "tag is-light has-text-weight-light my-0", ext)
                              )
                            )
                          )
                        )
                      )
                    )
                  )
                )
              }
            case _ =>
              div(cls := "message is-warning content",
                div(cls := "message-body",
                  "Torrents may contain ", strong("illegal, violent"), " or ", strong("adult"), " content. ",
                  "Open or download files on your ", strong("own risk"), "."
                )
              ) :: Nil
          }
      )
    )
  }
}