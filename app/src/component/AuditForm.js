import "../style/form.css";
import {useEffect, useState} from "react";
import api from "../axiosConfig";
import AnswerTemplate from "./AnswerTemplate";

function AuditForm({children, object, open, close}) {
    const [criteria, setCriteria] = useState([]);
    const [scores, setScores] = useState([]);
    const [activeCriteria, setActiveCriteria] = useState(null);
    const [isLoaded, setIsLoaded] = useState(false);

    const [score, setScore] = useState(null);

    const [answers, setAnswers] = useState([]);

    const emptyAnswer = {
        title: "",
        description: "",
        recommendation: "",
        comment: "",
    };

    const handleAnswerChange = (index, property, value) => {
        setAnswers(prev =>
            prev.map((a, i) =>
                i === index ? { ...a, [property]: value } : a
            )
        );
    };

    useEffect(() => {
        if (!activeCriteria) return;
        setScore(null);
        setAnswers([]);
    }, [activeCriteria]);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const res = await api.get(`/guidelines/` + children.id);
                const items = res.data.successCriteria
                setCriteria(items);
                setActiveCriteria(items[0]);
                setIsLoaded(true);
            } catch (err) {
                console.error(err);
            }
        }
        fetchData();
    }, [])

    useEffect(() => {
        const fetchData = async () => {
            try {
                const res = await api.get(`/audit/scores`);
                setScores(res.data)
            } catch (err) {
                console.error(err);
            }
        }
        fetchData();
    }, [])

    function selectNextCriteria() {
        const index = criteria.findIndex(c => c.refId === activeCriteria.refId);

        if (index !== -1 && index < criteria.length - 1) {
            setActiveCriteria(criteria[index + 1]);
        }
    }

    function selectPreviousCriteria() {
        const index = criteria.findIndex(c => c.refId === activeCriteria.refId);

        if (index > 0 && index < criteria.length - 1) {
            setActiveCriteria(criteria[index - 1]);
        } else {
            close();
        }
    }

    async function handleChange(e) {
        const formData = new FormData();

        for (let i = 0; i < e.target.files.length; i++) {
            formData.append("image", e.target.files[i]);
        }

        try {
            const res = await api.post(
                `/criteria/ai_picture?criteriaId=${activeCriteria.refId}&auditId=${object}`,
                formData,
                {
                    headers: {
                        "Content-Type": "multipart/form-data"
                    },
                }
            );
            setScore(res.data.overall_violation);
            setAnswers(dataMapper(res.data.violated_elements_and_reasons));
        } catch (err) {
            console.log(err);
        }
    }

    async function generateAnswer() {
        try {
            await api.get(`/criteria/ai_put?criteriaId=${activeCriteria.refId}&auditId=${object}`)
                .then(res => {
                    setScore(res.data.overall_violation);
                    setAnswers(dataMapper(res.data.violated_elements_and_reasons));
                });
        } catch (err) {
            console.log(err)
        }
    }

    function dataMapper(raw = []) {
        return raw.map(a => ({
            title: a.title ?? "",
            description: a.description ?? "",
            recommendation: a.recommendation ?? "",
            comment: "",
        }))
    }

    async function saveAnswer() {
        console.log(activeCriteria.refId)
        console.log(answers)
    }

    function addAnswer() {
        setAnswers(prev => [...prev, {...emptyAnswer}]);
    }

    function removeAnswer(index) {
        setAnswers(prev => prev.filter((_, i) => i !== index));
    }

    return (
        <div>
            {!isLoaded && (
                <div className="loading-container">
                    Loading...
                </div>
            )}
            {isLoaded && (
                <div>
                    <div className="header-title">
                        <div className="header-space">
                            <div className="text-space">
                                <div>
                                    <p>Audit</p>
                                    <i>{children.refId} {children.title}</i>
                                </div>
                                <div className="button-group">
                                    <button className="button-form"
                                            onClick={selectPreviousCriteria}>
                                        <i className="bi bi-arrow-bar-left"></i>
                                        Back
                                    </button>
                                    <button style={{"background-color": "#106DAA"}}
                                            className="button-form"
                                            onClick={() => saveAnswer()}>Save
                                        <i className="bi bi-floppy-fill"></i></button>
                                    <button className="button-form"
                                            onClick={selectNextCriteria}>
                                        Next
                                        <i className="bi bi-arrow-bar-right"></i></button>

                                </div>
                            </div>

                        </div>
                        <hr className="solid"/>
                    </div>

                    <div>
                        <form>
                            {activeCriteria && (
                                <div>
                                    <a href={activeCriteria.url} target="_blank" className="form-title">
                                        {activeCriteria.refId} {activeCriteria.title}
                                    </a>
                                    <p>{activeCriteria.description}</p>
                                </div>
                            )}
                            <div className="form-answer">
                                <i className="form-answer-title">Passed or failed?</i>
                                <div className="form-header">
                                    <select for="status">Choose:
                                        <option defaultChecked="choose">Choose:</option>
                                        {scores.map((s) => (
                                            <option>{s}</option>
                                        ))}
                                    </select>
                                    {activeCriteria.fetchType === "text" && (
                                        <div>
                                            <button type="button"
                                                    className="button-ai"
                                                    onClick={generateAnswer}>Generate with AI
                                                <i className="bi bi-stars"></i></button>
                                            <span
                                                title="Please note that the answers generated by AI are not 100% correct and you, as the auditor, should always double check the website yourself">
                                                                                <i className="bi bi-info-circle"></i>
                                                                            </span>
                                        </div>
                                    )}
                                    {activeCriteria.fetchType === "image" && (
                                        <div>
                                            <label htmlFor="ai-image-upload" className="button-ai">
                                                Upload one or more picture to generate with AI
                                                <i className="bi bi-stars"></i>
                                            </label>
                                            <input type="file" id="ai-image-upload" multiple onChange={handleChange}
                                                   accept="image/*"/>
                                            <span
                                                title="Please note that the answers generated by AI are not 100% correct and you, as the auditor, should always double check the website yourself">
                                                                                <i className="bi bi-info-circle"></i>
                                                                            </span>
                                        </div>
                                    )}
                                </div>

                                {answers.map((a, i) => (
                                    <div className="styling-form">
                                        <AnswerTemplate
                                            key={i}
                                            answerIndex={i}
                                            title={a.title}
                                            description={a.description}
                                            recommendation={a.recommendation}
                                            comment={a.comment}
                                            handleAnswerChange={handleAnswerChange}
                                        />
                                        <div>
                                            <button
                                                type="button"
                                                className="delete-button"
                                                onClick={() => removeAnswer(i)}>
                                                <i className="bi bi-trash3-fill"></i>
                                            </button>
                                        </div>
                                    </div>
                                ))}
                                <button type="button"
                                        className="button-add"
                                        onClick={addAnswer}>New
                                    <i className="bi bi-plus"></i></button>
                            </div>

                        </form>
                    </div>
                </div>

            )}
        </div>
    );
}

export default AuditForm;