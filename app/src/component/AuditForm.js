import "../style/form.css";

function AuditForm({ children, open, close }) {
    return (
        <div>
            <div>
                <div className="header-title">
                    <p>Audit</p>
                    <i>{children.refId} {children.title}</i>
                    <hr className="solid"/>
                </div>
            </div>

            <div>
                <form>
                    <select for="status">Choose:
                        <option defaultChecked="choose"></option>
                        <option>get from backend</option>
                    </select>
                        <div className="titleForm">
                            <label for="answerTitle">Title</label>
                            <input id="answerTitle" name="answerTitle" type="text"/>
                        </div>
                    <div>
                        <div className="titleForm">
                            <label htmlFor="answerDesc">Description</label>
                            <input id="answerDesc" name="answerDesc" type="text"/>
                        </div>
                        <div className="titleForm">
                            <label htmlFor="answerRec">Recommendation</label>
                            <input id="answerRec" name="answerRec" type="text"/>
                        </div>
                    </div>

                </form>
            </div>

            <button onClick={close}>Submit</button>
        </div>


    );
}

export default AuditForm;