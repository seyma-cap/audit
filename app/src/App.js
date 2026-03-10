import { Routes, Route } from 'react-router-dom';
import Audit from "./page/audit"
import Home from "./page/home"
import NavBar from './component/navbar';
import './style/App.css';
 
const App = () => {
   return (
      <>
      <NavBar/>
      <div class="body">
         <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/audit" element={<Audit />} />
         </Routes>
      </div>
      </>
   );
};
 
export default App;