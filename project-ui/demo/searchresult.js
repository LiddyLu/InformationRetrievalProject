import React from 'react';

class SearchResult extends React.Component {
    render() {
        return <div className="card">
            <div className="card-content">
            <h5 className="card-title"><a style={{display: "table-cell"}} target="_blank" href={this.props.url}>{this.props.title}</a></h5>
                <h6 className="grey-text">{this.props.revisioncount} edits in the past 3 months</h6>
            <p className="card-text"><div dangerouslySetInnerHTML={{ __html: this.props.abstract }} /></p>
            </div>
        </div>
    }
}

SearchResult.propTypes = {
    url: React.PropTypes.string,
    title: React.PropTypes.string,
    abstract: React.PropTypes.string
};

export default SearchResult;