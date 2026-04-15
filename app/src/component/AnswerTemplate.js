import "../style/AnswerTemplate.css"

function AnswerTemplate(props) {

    const onInputChange = (e) => {
        props.handleAnswerChange(props.answerIndex, e.target.name, e.target.value);
    }

    return (
        <div className="answer-div">
            <div className="titleForm">
                <label htmlFor="answerTitle">Title</label>
                <input
                    name="title"
                    id="answerTitle"
                    type="text"
                    value={props.title}
                    onChange={onInputChange}/>
            </div>
            <div>
                <div className="titleForm">
                    <label htmlFor="answerDesc">Description</label>
                    <textarea
                        name="description"
                        id="answerDesc"
                        rows="5"
                        value={props.description}
                        onChange={onInputChange}/>
                </div>
                <div className="titleForm">
                    <label htmlFor="answerRec">Recommendation</label>
                    <textarea
                        name="recommendation"
                        id="answerRec"
                        rows="5"
                        value={props.recommendation}
                        onChange={onInputChange}/>
                </div>
            </div>
            <div className="titleForm">
                <label htmlFor="answerCom">Comments</label>
                <textarea
                    name="comment"
                    id="answerCom"
                    rows="5"
                    value={props.comment}
                    onChange={onInputChange}/>
            </div>
        </div>
    );
}

export default AnswerTemplate;