import {BrowserRouter, Routes, Route} from 'react-router-dom';
import Audit from "./page/audit"
import Home from "./page/home"
import NavBar from './component/navbar';
import './style/App.css';

const App = () => {
    return (
        <>
            <BrowserRouter>
                <NavBar/>
                <div className="body">
                    <Routes>
                        <Route path="/" element={<Home/>}/>
                        <Route path="/audit" element={<Audit/>}/>
                        <Route path="/audit/:mainObjectId" element={<Audit/>}/>
                        <Route path="/audit/:mainObjectId/:guideRefId" element={<Audit/>}/>
                    </Routes>
                </div>
            </BrowserRouter>
        </>
    );
};

export default App;