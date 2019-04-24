import React from 'react';
import SearchResult from './searchresult';

class SolrConnectorDemo extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            solrSearchUrl: "http://localhost:8983/solr/wiki_core/select?bf=sqrt(log(revisioncount))&debug=all&defType=edismax&qf=content%20title%5E1.2&stopwords=true",
            query: "",
            limit: ""
        }
    }

    onSubmit(event) {
        event.preventDefault();
        let searchParams = {
            solrSearchUrl: this.state.solrSearchUrl,
            defType: "edismax",
            query: this.state.query,
            offset: 0,
            limit: this.state.limit === "" ? 10 : this.state.limit,
            fetchFields: ["id", "url", "title", "abstract", "revisioncount"],
            highlightParams: {
              "hl": "true",
              "hl.fl": ["content"],
              "hl.snippets": 1,
              "hl.fragsize": 300
            },
        };
        this.props.doSearch(searchParams);
    }

    isLetter(str) {
        return str.length === 1 && str.match(/[a-z]/i);
    }

    render() {
        return <div className="container">
            <h2>wikipedia search engine demo</h2>
            <form className="inputForm" onSubmit={this.onSubmit.bind(this)}>

                    <div className="input-field">
                        <label className="active" htmlFor="query">Query</label>
                        <input id="query" type="text" value={this.state.query}
                               onChange={e => {this.setState({ query: e.target.value })}} />
                    </div>
                    <div className="input-field">
                        <label className="active" htmlFor="query">Limit</label>
                        <input id="limit" type="number" value={this.state.limit}
                               onChange={e => {this.setState({ limit: e.target.value })}} />
                    </div>
                    <button className="btn btn-secondary" type="submit">Search</button>
            </form>
            <div>
                {
                    this.props.solrConnector.response && this.props.solrConnector.response.response.docs.map((doc, i) => {
                        //let abstract = this.props.solrConnector.response.response.highlighting[doc.id];
                        let abstract = doc.content;
                        if (this.props.solrConnector.response.highlighting[doc.id]) {
                            abstract = this.props.solrConnector.response.highlighting[doc.id].content[0];
                        }
                        abstract = abstract + "...";
                        // if (!(this.isLetter(abstract[0]) && abstract[0] === abstract[0].toUpperCase())) {
                        //     abstract = "..." + abstract;
                        // }
                        return <SearchResult url={doc.url} title={doc.title} abstract={abstract} revisioncount={doc.revisioncount} key={i}/>
                    })
                }
            </div>
        </div>;
    }
}

export default SolrConnectorDemo;
